/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.util.Map;
import java.util.regex.Matcher;
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
        this.serverRoot = Main.getServerRoot().getAbsolutePath();
        this.clientRoot = clientRoot;
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
     * Checks whether the given path is inside the jail (inside the common shared filesystem).
     * 
     * @param the
     *            path to check
     * @return <code>true</code> if the path is in the jail, <code>false</code> otherwise
     */
    public boolean isPathInJail(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        if (isPathAbsolute(path) && !path.startsWith(clientRoot)) {
            return false;
        }

        return true;
    }

    /**
     * Checks whether a path is absolute or not, either in windows and UNIX style.
     * 
     * @param path
     *            the path to check
     * @return whether the path looks absolute in one world or the other.
     */
    public boolean isPathAbsolute(String path) {
        return path.charAt(0) == '/' || (path.length() > 2 && path.charAt(1) == ':');
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
        String target = toServer ? serverRoot + "/" : clientRoot + "/";

        if (arg.contains(source)) {
            Pattern sourcePattern = Pattern.compile(source, Pattern.LITERAL);
            String result = sourcePattern.matcher(arg).replaceFirst(
                    Matcher.quoteReplacement(target));
            String style = target.contains("\\") ? "\\" : "/";
            return result.replaceAll("[/\\\\]+", Matcher.quoteReplacement(style));
        }
        return arg;
    }

}
