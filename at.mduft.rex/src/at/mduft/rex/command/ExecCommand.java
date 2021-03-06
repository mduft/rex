/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.mduft.rex.util.CrNlHelpFormatter;
import at.mduft.rex.util.HelpAppender;

/**
 * Command that allows execution of any command inside the shared file system on
 * the server. Extends {@link InvertedShellWrapper} to be able to sneak a custom
 * {@link InvertedShell} into it.
 */
public class ExecCommand extends InvertedShellWrapper {

	private static final Logger log = LoggerFactory
			.getLogger(ExecCommand.class);
	private static final OptionParser PARSER;
	private static final EnumSet<TtyOptions> TTY_UNIX = EnumSet
			.of(TtyOptions.ONlCr);
	private static final EnumSet<TtyOptions> TTY_WIN32 = EnumSet.of(
			TtyOptions.ICrNl, TtyOptions.ONlCr);

	private ExitCallback exit;
	private OutputStream err;
	private static ArgumentAcceptingOptionSpec<String> OPT_ROOT;
	private static ArgumentAcceptingOptionSpec<String> OPT_PWD;

	static {
		PARSER = new OptionParser();
		PARSER.formatHelpWith(CrNlHelpFormatter.INSTANCE);

		OPT_ROOT = PARSER
				.accepts(
						"roots",
						"mappings of server-paths to client-paths, each mapping sperated by ';', groups separated by ','")
				.withRequiredArg().describedAs("server-path;client-path,...")
				.withValuesSeparatedBy(',').required();
		OPT_PWD = PARSER
				.accepts("pwd",
						"path within mount point to set as current working directory")
				.withRequiredArg().describedAs("dir").required();
	}

	/**
	 * Creates a new {@link ExecCommand}. This will create the
	 * {@link ProcessExecutor} and pass it to the {@link InvertedShellWrapper}
	 * base. This might throw an Exception.
	 * 
	 * @param command
	 *            the raw command line passed from the client.
	 */
	public ExecCommand(String[] command) {
		super(createExecutor(command));
	}

	/**
	 * Creates a {@link ProcessExecutor} that is capable of handling execution
	 * of the given command.
	 * 
	 * @param command
	 *            the command as passed to the SSH server, split at argument
	 *            boundaries.
	 * @return the executor that is able to execute the given command.
	 */
	private static ProcessExecutor createExecutor(String[] command) {
		// argument 0 == exec, otherwise we would not be here...
		if (command.length < 1 || !"exec".equals(command[0])) {
			throw new IllegalArgumentException(
					"missing string 'exec' in first argument");
		}

		OptionSet opts;
		synchronized (PARSER) {
			opts = PARSER.parse(Arrays.copyOfRange(command, 1, command.length));
		}

		Map<String, String> rootMappings = ArgumentProcessor
				.getRootMappingsFromArgument(opts.valuesOf(OPT_ROOT));
		List<?> nonOpts = opts.nonOptionArguments();

		return new ProcessExecutor(nonOpts.toArray(new String[nonOpts.size()]),
				rootMappings, opts.valueOf(OPT_PWD),
				OsUtils.isUNIX() ? TTY_UNIX : TTY_WIN32);
	}

	@Override
	public void setErrorStream(OutputStream err) {
		super.setErrorStream(err);
		this.err = err;
	}

	@Override
	public void setExitCallback(ExitCallback callback) {
		super.setExitCallback(callback);
		this.exit = callback;
	}

	@Override
	public synchronized void start(Environment env) throws IOException {
		try {
			super.start(env);
		} catch (Exception e) {
			log.error("failed to execute", e);
			DefaultCommand c = new DefaultCommand(e);
			c.setErrorStream(err);
			c.setExitCallback(exit);
			c.start(env);
		}
	}

	@HelpAppender
	public static void appendHelp(StringBuilder builder) throws IOException {
		try (StringWriter wr = new StringWriter()) {
			PARSER.printHelpOn(wr);
			builder.append(wr.toString());
		}
	}
}
