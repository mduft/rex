/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import org.apache.sshd.server.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.mduft.rex.Main;
import at.mduft.rex.RexCommandFactory;

/**
 * The default {@link Command} that kicks in when everything else fails. This is either due to wrong
 * usage or due to an exception. In case an exception occured the messages the exception and all
 * it's causes are printed in the message passed to the client, along with the general information.
 */
public class DefaultCommand extends SimpleCommand {
    private static final Logger log = LoggerFactory.getLogger(DefaultCommand.class);
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
    public Integer call() {
        try {
            StringBuilder builder = new StringBuilder();
            builder.append("\r\n");
            builder.append("\t*** REX Remote EXecution Service ***\r\n");
            builder.append("\r\n");
            builder.append("\tLogin was successful, but you did not specify any (valid)\r\n");
            builder.append("\tcommand to execute. Following commands are available:\r\n");
            builder.append("\r\n");

            RexCommandFactory commandFactory = Main.getCommandFactory();
            for (Map.Entry<String, Class<? extends Command>> entry : commandFactory
                    .getRegisteredCommands().entrySet()) {
                builder.append('\t').append(entry.getKey()).append(":\r\n");
                try {
                    Method method = entry.getValue().getMethod("appendHelp", StringBuilder.class);
                    method.invoke(null, builder);
                } catch (Exception e) {
                    builder.append("\t[error creating help for " + entry.getKey() + "]\r\n");
                }
                builder.append("\r\n");
            }

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
        } catch (IOException e) {
            log.debug(e.toString());
        }
        return 1;
    }

}