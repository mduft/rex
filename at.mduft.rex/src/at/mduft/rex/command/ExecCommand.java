/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.EnumSet;

import org.apache.sshd.common.util.OsUtils;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.apache.sshd.server.shell.InvertedShellWrapper;
import org.apache.sshd.server.shell.ProcessShellFactory.TtyOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command that allows execution of any command inside the shared file system on the server.
 */
public class ExecCommand extends InvertedShellWrapper {

    private static final Logger log = LoggerFactory.getLogger(ExecCommand.class);
    private static final String ROOT_ARG = "--root=";
    private static final EnumSet<TtyOptions> TTY_UNIX = EnumSet.of(TtyOptions.ONlCr);
    private static final EnumSet<TtyOptions> TTY_WIN32 = EnumSet.of(TtyOptions.Echo,
            TtyOptions.ICrNl, TtyOptions.ONlCr);
    private ExitCallback exit;
    private OutputStream err;

    public ExecCommand(String[] command) {
        super(createExecutor(command));
    }

    /**
     * Creates a {@link ProcessExecutor} that is capable of handling execution of the given command.
     * The input command /must/ be in format <code>exec --root=... [real command ...]</code>.
     * 
     * @param command
     *            the command as passed to the SSH server, split at argument boundaries.
     * @return the executor that is able to execute the given command.
     */
    private static ProcessExecutor createExecutor(String[] command) {
        String clientRoot = null;

        if (command.length < 3) {
            throw new IllegalArgumentException("not a valid command string.");
        }

        if (!"exec".equals(command[0])) {
            throw new IllegalArgumentException("command string must start with 'exec'");
        }

        if (!command[1].startsWith(ROOT_ARG)) {
            throw new IllegalArgumentException("exec must be followed by " + ROOT_ARG);
        }

        clientRoot = command[1].substring(ROOT_ARG.length());

        if (clientRoot == null) {
            throw new IllegalArgumentException("cannot find " + ROOT_ARG
                    + " argument in command line from client");
        }

        return new ProcessExecutor(Arrays.copyOfRange(command, 2, command.length), clientRoot,
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
}
