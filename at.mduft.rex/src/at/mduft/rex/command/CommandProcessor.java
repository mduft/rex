/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.util.Map;

import at.mduft.rex.Main;

/**
 * The {@link CommandProcessor} is responsible for transforming a given command from client form to
 * server form, taking into account the
 */
public class CommandProcessor {

    private final Map<String, String> env;
    private final String clientRoot;
    private final String serverRoot;

    public CommandProcessor(String clientRoot, Map<String, String> env) {
        this.env = env;
        this.clientRoot = clientRoot;
        this.serverRoot = Main.getServerRoot();
    }

    /**
     * Process the given command line parts to exchange client side information with their server
     * side counterparts. Most work done here deals with fixing paths in command line arguments to
     * reflect the file path on the server that "means" the same as the original string on the
     * client.
     * 
     * @param original
     *            the original command line
     * @return the transformed parts of the command line.
     */
    public String[] process(String[] original) {
        if (!original[0].contains(clientRoot)) {
            throw new IllegalArgumentException(
                    "it is not allowed to escape prison (command to execute must be within "
                            + clientRoot + ")!");
        }

        String[] cmds = new String[original.length];
        for (int i = 0; i < cmds.length; i++) {
            if ("$USER".equals(original[i])) {
                cmds[i] = env.get("USER");
            } else {
                cmds[i] = transformPath(original[i]);
            }
        }
        return cmds;
    }

    /**
     * Transforms all paths in the given argument to the required target (server) style.
     * 
     * @param arg
     *            the argument to transform in client side style
     * @return the transformed argument in server side style
     */
    private String transformPath(String arg) {
        if (arg.contains(clientRoot)) {
            return arg.replace(clientRoot, serverRoot);
        }
        return arg;
    }

}
