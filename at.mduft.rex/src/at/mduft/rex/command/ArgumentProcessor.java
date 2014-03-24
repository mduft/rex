/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.common.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.mduft.rex.Main;

/**
 * The {@link ArgumentProcessor} is responsible for transforming a given command from client form to
 * server form, taking into account the
 */
public class ArgumentProcessor {

    private static final Logger log = LoggerFactory.getLogger(ArgumentProcessor.class);

    private static final String VAR_PATH = "PATH";
    private final Map<String, String> env;
    private final String clientRoot;
    private final String serverRoot;

    public ArgumentProcessor(String clientRoot, Map<String, String> env) {
        this.env = env;
        this.serverRoot = Main.getServerRoot().getAbsolutePath();
        this.clientRoot = clientRoot;
    }

    public boolean isServerWindows() {
        return OsUtils.isWin32();
    }

    public boolean isClientWindows() {
        return clientRoot.contains(":"); // path must be absolute, so if it contains a : it must be
                                         // windows.
    }

    public String getClientPathSep() {
        return isClientWindows() ? ";" : ":";
    }

    public String getServerPathSep() {
        return isServerWindows() ? ";" : ":";
    }

    /**
     * Process the given command line parts to exchange client side information with their server
     * side counterparts. Most work done here deals with fixing paths in command line arguments to
     * reflect the file path on the server that "means" the same as the original string on the
     * client.
     * <p>
     * Note that all given arguments are client relative, not server!
     * 
     * @param original
     *            the original command line
     * @param the
     *            current working directory
     * @return the transformed parts of the command line.
     */
    public String[] processArguments(String[] original, String pwd) {
        String[] cmds = new String[original.length];
        for (int i = 0; i < cmds.length; i++) {
            if ("$USER".equals(original[i])) {
                cmds[i] = env.get("USER");
            } else {
                cmds[i] = transformPath(original[i], true);
            }
        }

        if (cmds[0].startsWith(".")) {
            cmds[0] = transformPath(pwd + cmds[0].substring(1), true);
        }
        return cmds;
    }

    /**
     * Process the given environment, exchanging client side paths with server side ones where
     * applicable.
     * 
     * @param environment
     *            the environment to process.
     */
    public void processEnvironment(Map<String, String> environment) {
        // only PATH is valid. windows may set and use path with different case.
        Set<String> badVars = new HashSet<>();
        for (Map.Entry<String, String> entry : environment.entrySet()) {
            String key = entry.getKey();
            if (key.equalsIgnoreCase(VAR_PATH) && !key.equals(VAR_PATH)) {
                badVars.add(key);
            }
        }

        for (String bad : badVars) {
            environment.remove(bad);
        }

        processPath(environment);
    }

    private void processPath(Map<String, String> environment) {
        String serverPath = System.getenv(VAR_PATH);
        String clientPath = environment.get(VAR_PATH);

        if (clientPath == null || clientPath.isEmpty()) {
            environment.put(VAR_PATH, serverPath); // use default path only
        }

        StringBuilder finalPath = new StringBuilder(serverPath);
        String clientPaths[] = clientPath.split(getClientPathSep());
        for (String p : clientPaths) {
            if (p == null || p.isEmpty()) {
                continue;
            }

            if (isPathInJail(p)) {
                finalPath.append(getServerPathSep()).append(transformPath(p, true));
            } else {
                log.debug("skip out-of-jail PATH member: " + p);
            }
        }

        environment.put(VAR_PATH, finalPath.toString());
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
    public static boolean isPathAbsolute(String path) {
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
