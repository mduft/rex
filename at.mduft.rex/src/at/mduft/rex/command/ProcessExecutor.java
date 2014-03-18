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
 * Can execute arbitrary commands. Basically an adapted copy of {@link ProcessShell}, to get rid of
 * its binding to {@link ProcessShellFactory}. The adaption relative to the original class is:
 * <ul>
 * <li>Made command part of this class instead of the factory</li>
 * <li>Added/changed some logging</li>
 * <li>Added command processing to get clientRoot -> serverRoot conversion</li>
 * </ul>
 */
public class ProcessExecutor implements InvertedShell {

    private static final Logger log = LoggerFactory.getLogger(ProcessExecutor.class);

    private Process process;
    private TtyFilterOutputStream in;
    private TtyFilterInputStream out;
    private TtyFilterInputStream err;

    private final String[] command;
    private final EnumSet<TtyOptions> ttyOptions;
    private final String clientRoot;

    private final String targetPwd;

    public ProcessExecutor(String[] command, String clientRoot, String targetPwd,
            EnumSet<TtyOptions> options) {
        this.command = command;
        this.ttyOptions = options;
        this.clientRoot = clientRoot;
        this.targetPwd = targetPwd;
    }

    @Override
    public void start(Map<String, String> env) throws IOException {
        ArgumentProcessor proc = new ArgumentProcessor(clientRoot, env);
        String[] cmds = proc.process(command);
        ProcessBuilder builder = new ProcessBuilder(cmds);
        if (env != null) {
            try {
                builder.environment().putAll(env);
            } catch (Exception e) {
                log.info("Could not set environment for command", e);
            }
        }
        builder.directory(new File(proc.transformPath(targetPwd, true)));
        log.info("starting '{}'", builder.command());
        if (log.isDebugEnabled()) {
            log.debug("Starting process with command: '{}' and env: {}", builder.command(),
                    builder.environment());
        }
        process = builder.start();
        out = new TtyFilterInputStream(process.getInputStream());
        err = new TtyFilterInputStream(process.getErrorStream());
        in = new TtyFilterOutputStream(process.getOutputStream(), err);
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
        if (process == null) {
            return false;
        }

        try {
            process.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    @Override
    public int exitValue() {
        if (process == null) {
            return -1;
        }
        try {
            return process.waitFor();
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
            if (c == '\n' && ttyOptions.contains(TtyOptions.ONlCr) && lastChar != '\r') {
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
        public synchronized int read(byte[] b, int off, int len) throws IOException {
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
