/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex;

import java.io.File;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the REX server
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final Options options;

    /** The default port used if no other is given */
    private static final int DEFAULT_PORT = 9000;

    /** name of the option specifying the path to the authorized public key repository */
    private static final String OPT_PUBKEYS = "pubkeys";

    /** name of the option specifying the port the server should listen on */
    private static final String OPT_PORT = "port";

    /** name of the option specifying the (reusable) host key file */
    private static final String OPT_HOSTKEY = "hostkey";

    /** name of the option specifying the username that is allowed to connect */
    private static final String OPT_USER = "user";

    /** name of the option specifying the mount point/share directory of the shared storage */
    private static final String OPT_ROOT = "root";

    static {
        options = new Options();
        Option port = new Option("p", OPT_PORT, true, "The port to start the server on (default: "
                + DEFAULT_PORT + ")");
        Option pubKey = new Option(null, OPT_PUBKEYS, true,
                "File containing authorized public keys");
        Option hostKey = new Option(null, OPT_HOSTKEY, true,
                "Host key file, created if it does not exist");
        Option user = new Option(null, OPT_USER, true,
                "User name required to be used by connecting clients. Clients cannot connect with any other user.");
        Option root = new Option("r", OPT_ROOT, true,
                "Path to mount point/share of common filesystem with client machines");

        hostKey.setRequired(true);
        pubKey.setRequired(true);
        user.setRequired(true);
        root.setRequired(true);

        pubKey.setArgName("authorized_keys2");
        hostKey.setArgName("hostkey-file");
        user.setArgName("permitted-user");

        options.addOption(port);
        options.addOption(pubKey);
        options.addOption(hostKey);
        options.addOption(user);
        options.addOption(root);
    }

    private static String serverRoot = null;

    /**
     * Main entry point into the application. Sets up global things and starts the SSH server. The
     * main thread will quit once the setup is done.
     * 
     * @param args
     *            command line arguments for the server
     * @throws Exception
     *             in case of an unexpected error.
     */
    public static void main(String[] args) throws Exception {
        int port = DEFAULT_PORT;

        // parse command line and print help in case we cannot.
        CommandLine cli;
        try {
            Parser parser = new PosixParser();
            cli = parser.parse(options, args);
        } catch (Exception e) {
            log.info("error: " + e.toString());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(120, "Main", null, options, null, true);
            System.exit(-1);
            return;
        }

        // remember the server root mapping.
        serverRoot = cli.getOptionValue(OPT_ROOT);
        if (!new File(serverRoot).isDirectory()) {
            throw new IllegalArgumentException("server root directory must exist");
        }

        // setup the ssh server with some defaults and the options given on the command line.
        log.info("starting REX server");
        SshServer server = SshServer.setUpDefaultServer();

        if (cli.hasOption(OPT_PORT)) {
            port = Integer.parseInt(cli.getOptionValue(OPT_PORT));
        }

        File pubKeyFile = new File(cli.getOptionValue(OPT_PUBKEYS));
        if (!pubKeyFile.isFile()) {
            throw new IllegalArgumentException("argument not a file: " + pubKeyFile);
        }
        PubKeyAuthenticator auth = new PubKeyAuthenticator(cli.getOptionValue(OPT_USER), pubKeyFile);
        server.setPublickeyAuthenticator(auth);

        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(cli
                .getOptionValue(OPT_HOSTKEY)));

        Map<String, String> props = server.getProperties();
        props.put(SshServer.AUTH_METHODS, "publickey");
        props.put(SshServer.SERVER_IDENTIFICATION, "REX-Service");

        server.setShellFactory(new RexShellFactory());
        server.setCommandFactory(new RexCommandFactory());

        server.setPort(port);
        server.start();
    }

    public static String getServerRoot() {
        return serverRoot;
    }
}
