/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.File;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.Map;

import org.apache.sshd.common.util.Buffer;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.ProcessShellFactory;
import org.apache.sshd.server.shell.ProcessShellFactory.ProcessShell;
import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Can execute arbitrary commands. Basically an adapted copy of
 * {@link ProcessShell}, to get rid of its binding to
 * {@link ProcessShellFactory}. The adaption relative to the original class is:
 * <ul>
 * <li>Made command part of this class instead of the factory</li>
 * <li>Added/changed some logging</li>
 * <li>Added command processing to get clientRoot -> serverRoot conversion</li>
 * </ul>
 */
public class ProcessExecutor implements InvertedShell {

	private static final Logger log = LoggerFactory
			.getLogger(ProcessExecutor.class);

	/** the process or null if not yet executing */
	private Process process;

	/** servers input side, connected to process' stdout */
	private TtyFilterOutputStream in;

	/** servers output side, connected to process' stdin */
	private TtyFilterInputStream out;

	/** servers error side, connected to process' stderr */
	private TtyFilterInputStream err;

	/** the raw command passed by the client */
	private final String[] command;

	/** the {@link TtyOptions} used for the streams connected to the process */
	private final EnumSet<TtyOptions> ttyOptions;

	/** the current working directory on the client */
	private final String clientPwd;

	private final ArgumentProcessor proc;

	/**
	 * Creates a new {@link ProcessExecutor}.
	 * 
	 * @param command
	 *            the command to execute. has to be at least of length 1
	 * @param rootMappings
	 *            the (absolute!) paths mapped to each other, client path is
	 *            key, server is value.
	 * @param clientPwd
	 *            the (client) current working directory. must be within the
	 *            shared filesystem
	 * @param options
	 *            the {@link TtyOptions} to be used for streams.
	 */
	public ProcessExecutor(String[] command, Map<String, String> rootMappings,
			String clientPwd, EnumSet<TtyOptions> options) {
		this.command = command;
		this.ttyOptions = options;
		this.clientPwd = clientPwd;
		this.proc = new ArgumentProcessor(rootMappings);
	}

	@Override
	public void start(Map<String, String> env) throws IOException {
		checkSetup(proc);

		ProcessBuilder builder = new ProcessBuilder();
		String[] cmds = proc.process(command, clientPwd, env,
				builder.environment());
		builder.command(cmds);
		builder.directory(new File(proc.transformPath(clientPwd, true)));

		log.info("starting '{}'", builder.command());
		long start = System.currentTimeMillis();
		process = builder.start();
		out = new TtyFilterInputStream(process.getInputStream());
		err = new TtyFilterInputStream(process.getErrorStream());
		in = new TtyFilterOutputStream(process.getOutputStream(), err);
		System.out.println("start took " + (System.currentTimeMillis() - start) + "ms.");
	}

	/**
	 * Verifies that the current setup is ok to be executed.
	 * 
	 * @param proc
	 *            the {@link ArgumentProcessor} used to check constraints.
	 */
	private void checkSetup(ArgumentProcessor proc) {
		if (command == null || command.length < 1) {
			throw new IllegalArgumentException("no command given");
		}

		if (!proc.isPathInJail(command[0]) && ArgumentProcessor.isPathAbsolute(command[0])) {
			throw new IllegalArgumentException(
					"it is not allowed to escape prison (executable (" + command[0] + ") must be within one of the mapped paths)!");
		}

		if (!ArgumentProcessor.isPathAbsolute(clientPwd)
				|| !proc.isPathInJail(clientPwd)) {
			throw new IllegalArgumentException(
					"it is not allowed to escape prison (current directory (" + clientPwd + ") must be within one of the mapped paths)!");
		}
	}

	@Override
	public OutputStream getInputStream() {
		return in;
	}

	@Override
	public InputStream getOutputStream() {
		return out;
	}

	@Override
	public InputStream getErrorStream() {
		return err;
	}

	@Override
	public boolean isAlive() {
		log.trace("check alive");
		if (process == null) {
			return false;
		}

		try {
			process.exitValue();
			log.trace("no");
			return false;
		} catch (IllegalThreadStateException e) {
			log.trace("yes");
			return true;
		}
	}

	@Override
	public int exitValue() {
		log.trace("get exit value");
		if (process == null) {
			return -1;
		}
		try {
			int status = process.waitFor();
			log.info(command[0] + " done, status=" + status);
			return status;
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void destroy() {
		if (process != null) {
			process.destroy();
		}
	}

	/**
	 * Copy of {@link ProcessShell}s {@code TtyFilterInputStream}.
	 */
	protected class TtyFilterInputStream extends FilterInputStream {
		private Buffer buffer;
		private int lastChar;

		public TtyFilterInputStream(InputStream in) {
			super(in);
			buffer = new Buffer(32);
		}

		synchronized void write(int c) {
			buffer.putByte((byte) c);
		}

		synchronized void write(byte[] buf, int off, int len) {
			buffer.putBytes(buf, off, len);
		}

		@Override
		public int available() throws IOException {
			return super.available() + buffer.available();
		}

		@Override
		public synchronized int read() throws IOException {
			int c;
			if (buffer.available() > 0) {
				c = buffer.getByte();
				buffer.compact();
			} else {
				c = super.read();
			}
			if (c == '\n' && ttyOptions.contains(TtyOptions.ONlCr)
					&& lastChar != '\r') {
				c = '\r';
				Buffer buf = new Buffer();
				buf.putByte((byte) '\n');
				buf.putBuffer(buffer);
				buffer = buf;
			} else if (c == '\r' && ttyOptions.contains(TtyOptions.OCrNl)) {
				c = '\n';
			}
			lastChar = c;
			return c;
		}

		@Override
		public synchronized int read(byte[] b, int off, int len)
				throws IOException {
			if (buffer.available() == 0) {
				int nb = super.read(b, off, len);
				buffer.putRawBytes(b, off, nb);
			}
			int nb = 0;
			while (nb < len && buffer.available() > 0) {
				b[off + nb++] = (byte) read();
			}
			return nb;
		}
	}

	/**
	 * Copy of {@link ProcessShell}s {@code TtyFilterOutputStream}.
	 */
	protected class TtyFilterOutputStream extends FilterOutputStream {
		private final TtyFilterInputStream echo;

		public TtyFilterOutputStream(OutputStream out, TtyFilterInputStream echo) {
			super(out);
			this.echo = echo;
		}

		@Override
		public void write(int c) throws IOException {
			if (c == '\n' && ttyOptions.contains(TtyOptions.INlCr)) {
				c = '\r';
			} else if (c == '\r' && ttyOptions.contains(TtyOptions.ICrNl)) {
				c = '\n';
			}
			super.write(c);
			if (ttyOptions.contains(TtyOptions.Echo)) {
				echo.write(c);
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			for (int i = off; i < len; i++) {
				write(b[i]);
			}
		}
	}
}
