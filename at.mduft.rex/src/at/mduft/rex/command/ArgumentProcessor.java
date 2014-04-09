/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sshd.common.util.OsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ArgumentProcessor} is responsible for transforming a given command
 * from client form to server form, taking into account the
 */
public class ArgumentProcessor {

	private static final Logger log = LoggerFactory
			.getLogger(ArgumentProcessor.class);
	private static final Pattern SLASH_FIXER = Pattern.compile("[/\\\\]+");
	private static final String VAR_PATH = "PATH";
	private static final List<String> BAD_VARS = Arrays.asList("TMP", "TEMP");

	/**
	 * holds mappings of client path to server path. client path is the key,
	 * server path the value!
	 */
	private final Map<String, String> rootMappings;

	/**
	 * Creates a new {@link ArgumentProcessor} for the given client root and the
	 * given environment.
	 * 
	 * @param clientRoot
	 *            the client's path to the common file system mount point.
	 */
	public ArgumentProcessor(Map<String, String> rootMappings) {
		this.rootMappings = rootMappings;
	}

	/**
	 * @return whether the REX server is running on windows
	 */
	public boolean isServerWindows() {
		return OsUtils.isWin32();
	}

	/**
	 * @return whether the connected client is running on windows
	 */
	public boolean isClientWindows() {
		// all paths must be absolute, so if any one contains a : it must be
		// windows.
		return rootMappings.keySet().iterator().next().contains(":");
	}

	/**
	 * @return the separator for path lists in environment variables on the
	 *         client.
	 */
	public String getClientPathSep() {
		return isClientWindows() ? ";" : ":";
	}

	/**
	 * @return the separator for path lists in environment variables on the
	 *         server.
	 */
	public String getServerPathSep() {
		return isServerWindows() ? ";" : ":";
	}

	/**
	 * Process the given command line parts to exchange client side information
	 * with their server side counterparts. Most work done here deals with
	 * fixing paths in command line arguments to reflect the file path on the
	 * server that "means" the same as the original string on the client.
	 * <p>
	 * Note that all given arguments are client relative, not server!
	 * 
	 * @param original
	 *            the original command line
	 * @param pwd
	 *            the current working directory
	 * @param env
	 *            the unprocessed environment. will be processed (path
	 *            conversions, ...)
	 * @param targetEnv
	 *            the target environment to copy processed variables to.
	 * @return the transformed parts of the command line.
	 */
	public String[] process(String[] original, String pwd,
			Map<String, String> env, Map<String, String> targetEnv) {
		processEnvironment(env, targetEnv);

		String[] cmds = new String[original.length];
		for (int i = 0; i < cmds.length; i++) {
			if ("$USER".equals(original[i])) {
				cmds[i] = env.get("USER");
			} else {
				cmds[i] = transformPath(original[i], true);
			}
		}

		if (cmds[0].startsWith("..")) {
			cmds[0] = transformPath(pwd + "/" + cmds[0], true);
		} else if (cmds[0].startsWith(".")) {
			cmds[0] = transformPath(pwd + cmds[0].substring(1), true);
		}
		return cmds;
	}

	/**
	 * Process the given environment, exchanging client side paths with server
	 * side ones where applicable.
	 * 
	 * @param environment
	 *            the environment to process.
	 */
	private void processEnvironment(Map<String, String> environment,
			Map<String, String> target) {
		// only PATH is valid. windows sets and uses path with different case.
		// merge them
		for (Map.Entry<String, String> entry : environment.entrySet()) {
			String key = entry.getKey();
			if (key.equalsIgnoreCase(VAR_PATH) && !key.equals(VAR_PATH)) {
				continue;
			}

			if (BAD_VARS.contains(key)) {
				continue;
			}
			
			if(key.startsWith("_REX_")) {
				key = key.substring("_REX_".length());
			}
			
			target.put(key, entry.getValue());
		}

		// build rex root information and export.
		StringBuilder builder = new StringBuilder();
		for (Map.Entry<String, String> mapping : rootMappings.entrySet()) {
			if (builder.length() > 0) {
				builder.append(',');
			}
			builder.append(mapping.getValue()).append(';')
					.append(mapping.getKey());
		}
		target.put("REX_ROOTS", builder.toString());

		processPath(target);
	}

	/**
	 * Cleans up the PATH variable.
	 * 
	 * @param environment
	 *            the environment to process.
	 */
	private void processPath(Map<String, String> environment) {
		String serverPath = System.getenv(VAR_PATH);
		String clientPath = environment.get(VAR_PATH);

		if (clientPath == null || clientPath.isEmpty()) {
			environment.put(VAR_PATH, serverPath); // use default path only
			return;
		}

		StringBuilder finalPath = new StringBuilder(serverPath);
		String clientPaths[] = clientPath.split(getClientPathSep());
		for (String p : clientPaths) {
			if (p == null || p.isEmpty()) {
				continue;
			}

			if (isPathInJail(p)) {
				finalPath.append(getServerPathSep()).append(
						transformPath(p, true));
			} else {
				log.trace("skip out-of-jail PATH member: " + p);
			}
		}

		environment.put(VAR_PATH, finalPath.toString());
	}

	/**
	 * Checks whether the given path is inside the jail (inside the common
	 * shared filesystem).
	 * 
	 * @param the
	 *            path to check
	 * @return <code>true</code> if the path is in the jail, <code>false</code>
	 *         otherwise
	 */
	public boolean isPathInJail(String path) {
		if (path == null || path.isEmpty()) {
			return false;
		}

		for (Map.Entry<String, String> mapping : rootMappings.entrySet()) {
			if (isPathAbsolute(path) && path.startsWith(mapping.getKey())) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether a path is absolute or not, either in windows and UNIX
	 * style.
	 * 
	 * @param path
	 *            the path to check
	 * @return whether the path looks absolute in one world or the other.
	 */
	public static boolean isPathAbsolute(String path) {
		return path.charAt(0) == '/'
				|| (path.length() > 2 && path.charAt(1) == ':');
	}

	/**
	 * Transforms all paths in the given argument to the required target
	 * (server) style.
	 * 
	 * @param arg
	 *            the argument to transform in client side style
	 * @param toServer
	 *            whether to convert from client to server or the other way
	 *            round.
	 * @return the transformed argument in server side style
	 */
	public String transformPath(String arg, boolean toServer) {
		for (Map.Entry<String, String> mapping : rootMappings.entrySet()) {
			String source = toServer ? mapping.getKey() : mapping.getValue();
			String target = toServer ? mapping.getValue() + "/" : mapping
					.getKey() + "/";

			Pattern sourcePattern = Pattern.compile("(?<![/\\\\w\\d])"
					+ Pattern.quote(source));
			Matcher matcher = sourcePattern.matcher(arg);
			if (matcher.find()) {
				String result = matcher.replaceFirst(Matcher
						.quoteReplacement(target));
				String style = Matcher
						.quoteReplacement(target.contains("\\") ? "\\" : "/");

				Matcher slash = SLASH_FIXER.matcher(result);
				boolean hasSlash = slash.find(matcher.start());
				if (hasSlash) {
					StringBuffer builder = new StringBuffer();
					do {
						slash.appendReplacement(builder, style);
						hasSlash = slash.find();
					} while (hasSlash);
					slash.appendTail(builder);
					result = builder.toString();
				}
				return result;
			}
		}
		return arg;
	}

	/**
	 * Split raw root mappings into a {@link Map}, client path as key, server
	 * path as value.
	 */
	public static Map<String, String> getRootMappingsFromArgument(
			List<String> rawMappings) {
		Map<String, String> rootMappings = new TreeMap<>();

		// TODO: check for "allowed" to be save against jailbreaks!

		for (String raw : rawMappings) {
			String split[] = raw.split(";");
			if (split.length != 2) {
				throw new IllegalStateException("invalid roots argument: '"
						+ raw + "', should be 'server-path;client-path'");
			}

			for (int i = 0; i < 2; ++i) {
				if (!isPathAbsolute(split[i])) {
					throw new IllegalStateException(
							"only absolute paths are valid for roots, got: "
									+ split[i]);
				}
			}

			// client path is key for faster lookup, server path is value.
			rootMappings.put(split[1], split[0]);
		}
		return rootMappings;
	}

}
