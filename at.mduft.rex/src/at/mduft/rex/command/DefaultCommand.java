/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

/**
 * The default {@link Command} that kicks in when everything else fails. This is either due to wrong
 * usage or due to an exception. In case an exception occured the messages the exception and all
 * it's causes are printed in the message passed to the client, along with the general information.
 */
public class DefaultCommand implements Command {
    private OutputStream err;
    private ExitCallback exit;
    private final Throwable exception;

    /**
     * Creates a {@link DefaultCommand} that outputs some informative text and then exits with an
     * error code.
     */
    public DefaultCommand() {
        this(null);
    }

    /**
     * Creates a {@link DefaultCommand} with additional error information. This can be used if
     * another command fails to display error information to the user.
     * 
     * @see DefaultCommand#DefaultCommand().
     * @param e
     *            the {@link Throwable} to log to the user.
     */
    public DefaultCommand(Throwable e) {
        this.exception = e;
    }

    @Override
    public void start(Environment env) throws IOException {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\r\n");
            builder.append("\t*** REX Remote EXecution Service ***\r\n");
            builder.append("\r\n");
            builder.append("\tLogin was successful, but you did not specify any (valid)\r\n");
            builder.append("\tcommand to execute. Following commands are available:\r\n");
            builder.append("\r\n");
            builder.append("\t  exec --root=<root> <cmd...>\r\n");
            builder.append("\t\t\texecutes the given command on the server machine.\r\n");
            builder.append("\t\t\t'root' specifies the mount point of a common\r\n");
            builder.append("\t\t\tfilesystem on the client machine. The according\r\n");
            builder.append("\t\t\tmapping on the server has to be specified at startup\r\n");
            builder.append("\t\t\tof the server process.\r\n");

            if (exception != null) {
                builder.append("\r\n");
                builder.append("\tProblem occured:\r\n");
                Throwable current = exception;
                String indent = "  ";
                while (current != null) {
                    builder.append("\t" + indent + current.toString() + "\r\n");
                    if (current.getCause() == current) {
                        break;
                    }
                    current = current.getCause();
                    indent += "  ";
                }
            }

            builder.append("\r\n");
            err.write(builder.toString().getBytes());
            err.flush();
        } finally {
            exit.onExit(127);
        }
    }

    @Override
    public void setOutputStream(OutputStream out) {
    }

    @Override
    public void setInputStream(InputStream in) {
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.exit = callback;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void destroy() {
    }
}