/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.util.Map;
import java.util.regex.Pattern;

import at.mduft.rex.Main;

/**
 * The {@link ArgumentProcessor} is responsible for transforming a given command from client form to
 * server form, taking into account the
 */
public class ArgumentProcessor {

    private final Map<String, String> env;
    private final String clientRoot;
    private final String serverRoot;

    public ArgumentProcessor(String clientRoot, Map<String, String> env) {
        this.env = env;
        String absRoot = Main.getServerRoot().getAbsolutePath();

        if (absRoot.endsWith("/") || absRoot.endsWith("\\")) {
            serverRoot = absRoot;
        } else {
            serverRoot = absRoot + "/";
        }

        if (clientRoot.endsWith("/") || clientRoot.endsWith("\\")) {
            this.clientRoot = clientRoot;
        } else {
            this.clientRoot = clientRoot + "/";
        }
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
                cmds[i] = transformPath(original[i], true);
            }
        }
        return cmds;
    }

    /**
     * Transforms all paths in the given argument to the required target (server) style.
     * 
     * @param arg
     *            the argument to transform in client side style
     * @param toServer
     *            whether to convert from client to server or the other way round.
     * @return the transformed argument in server side style
     */
    public String transformPath(String arg, boolean toServer) {
        String source = toServer ? clientRoot : serverRoot;
        String target = toServer ? serverRoot : clientRoot;

        if (arg.contains(source)) {
            Pattern sourcePattern = Pattern.compile(source, Pattern.LITERAL);
            String result = sourcePattern.matcher(arg).replaceFirst(target);
            String style = target.contains("\\") ? "\\" : "/";
            return result.replaceAll("[/\\\\]+", style);
        }
        return arg;
    }

}
