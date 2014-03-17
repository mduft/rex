/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.mduft.rex.command.DefaultCommand;
import at.mduft.rex.command.ExecCommand;

/**
 * Static command factory for REX commands. Each supported command has to be added here.
 */
public class RexCommandFactory implements CommandFactory {

    private static final Logger log = LoggerFactory.getLogger(RexCommandFactory.class);

    /** stores all supported commands, each being instantiated freshly for each request. */
    private static final Map<String, Class<? extends Command>> commands;

    static {
        commands = new HashMap<>();
        commands.put("exec", ExecCommand.class);
    }

    @Override
    public Command createCommand(String command) {
        String[] args = splitCommand(command);

        Class<? extends Command> cls = commands.get(args[0]);
        if (cls == null) {
            return new DefaultCommand();
        }

        try {
            // try with String[] parameter
            for (Constructor<?> x : cls.getConstructors()) {
                Class<?>[] parameterTypes = x.getParameterTypes();
                if (parameterTypes.length == 1 && parameterTypes[0].equals(String[].class)) {
                    return (Command) x.newInstance((Object) args);
                }
            }

            // try without any parameter
            return cls.newInstance();
        } catch (Exception e) {
            log.error("cannot create command " + cls.getName(), e);
            return new DefaultCommand(e);
        }
    }

    /**
     * Split command into separate pieces, also taking quoting into account.
     * 
     * @param command
     *            the command as a single string.
     * @return the command split into separate parts.
     */
    private String[] splitCommand(String command) {
        // TODO: better splitting (quoting, ...)
        return command.split(" ");
    }

}
