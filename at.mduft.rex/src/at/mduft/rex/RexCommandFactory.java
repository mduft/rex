/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.server.Command;
import org.apache.sshd.server.CommandFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.mduft.rex.command.DefaultCommand;
import at.mduft.rex.command.ExecCommand;
import at.mduft.rex.command.PathConvCommand;

/**
 * Static command factory for REX commands. Each supported command has to be added here.
 */
public class RexCommandFactory implements CommandFactory {

    private static final Logger log = LoggerFactory.getLogger(RexCommandFactory.class);

    /**
     * Pattern that is capable of dealing with complex command line quoting and escaping. This can
     * recognize correctly:
     * <ul>
     * <li>"double quoted strings"
     * <li>'single quoted strings'
     * <li>"escaped \"quotes within\" quoted string"
     * <li>C:\paths\like\this or "C:\path like\this"
     * <li>--arguments=like_this or "--args=like this" or '--args=like this'
     * <li>quoted\ whitespaces\\t (spaces & tabs)
     * <li>and probably more :)
     * </ul>
     */
    private static final Pattern CLI_CRACKER = Pattern.compile(
            "\"(\\\\+\"|[^\"])*?\"|'(\\\\+'|[^'])*?'|(\\\\\\s|[^\\s])+", Pattern.MULTILINE);

    /**
     * Cleans out quotes that are not escaped. Also removes single backslashes that quote a
     * whitespace.
     */
    private static final Pattern CLI_UNQUOTER = Pattern.compile(
            "(?<!\\\\)[\"']|(?<!\\\\)\\\\(?=\\s)", Pattern.MULTILINE);

    /** stores all supported commands, each being instantiated freshly for each request. */
    private static final Map<String, Class<? extends Command>> commands;

    static {
        commands = new HashMap<>();
        commands.put("exec", ExecCommand.class);
        commands.put("path", PathConvCommand.class);
    }

    @Override
    public Command createCommand(String command) {
        String[] args = splitAndCleanCommand(command);

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
            log.error("cannot create command " + cls.getName() + " for: " + Arrays.asList(args));
            Throwable current = e;
            String indent = "  ";
            while (current != null) {
                log.error(indent + current.toString());
                if (current.getCause() == current) {
                    break;
                }
                current = current.getCause();
                indent += "  ";
            }
            return new DefaultCommand(e);
        }
    }

    /**
     * @return all registered commands
     */
    public Map<String, Class<? extends Command>> getRegisteredCommands() {
        return commands;
    }

    /**
     * Split command into separate pieces, also taking quoting into account.
     * 
     * @param command
     *            the command as a single string.
     * @return the command split into separate parts.
     */
    private static String[] splitAndCleanCommand(String command) {
        if (command == null || command.isEmpty()) {
            return new String[0];
        }

        List<String> cracked = new ArrayList<>();
        Matcher matcher = CLI_CRACKER.matcher(command);
        while (matcher.find()) {
            String unquoted = CLI_UNQUOTER.matcher(matcher.group()).replaceAll("");
            cracked.add(unquoted.replace("\\\\", "\\"));
        }
        return cracked.toArray(new String[cracked.size()]);
    }

}
