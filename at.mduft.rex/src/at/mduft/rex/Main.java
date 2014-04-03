/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point for the REX server
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final OptionParser PARSER;
    private static final RexCommandFactory COMMAND_FACTORY = new RexCommandFactory();

    /** The default port used if no other is given */
    private static final int DEFAULT_PORT = 9000;

    /** high idle timeout (for master connections) - 60 minutes */
    private static final int IDLE_MASTER_TIMEOUT = 60 * 60 * 1000;

    private static final ArgumentAcceptingOptionSpec<Integer> OPT_PORT;
    private static final ArgumentAcceptingOptionSpec<File> OPT_PUBKEYS;
    private static final ArgumentAcceptingOptionSpec<String> OPT_USER;
    private static final ArgumentAcceptingOptionSpec<File> OPT_HOSTKEY;

    static {
        PARSER = new OptionParser();

        OPT_PORT = PARSER.acceptsAll(Arrays.asList("port", "p"), "The port to start the server on")
                .withRequiredArg().ofType(Integer.class).describedAs("port")
                .defaultsTo(DEFAULT_PORT);
        OPT_PUBKEYS = PARSER
                .acceptsAll(Arrays.asList("pubkeys", "k"), "File containing authorized public keys")
                .withRequiredArg().ofType(File.class).describedAs("pubkeys").required();
        OPT_USER = PARSER
                .acceptsAll(Arrays.asList("user", "u"),
                        "User name required to be used by connecting clients. Clients cannot connect with any other user.")
                .withRequiredArg().describedAs("username").defaultsTo("rex");
        OPT_HOSTKEY = PARSER
                .acceptsAll(Arrays.asList("hostkey", "h"),
                        "Host key file, created if it does not exist").withRequiredArg()
                .ofType(File.class).describedAs("hostkey-storage").required();
        PARSER.acceptsAll(Arrays.asList("help", "?"), "show this help").forHelp();
    }

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
        // parse command line and print help in case we cannot.
        OptionSet opts;
        try {
            opts = PARSER.parse(args);

            if (opts.has("help")) {
                PARSER.printHelpOn(System.out);
                System.exit(0);
            }
        } catch (Exception e) {
            log.info("error: " + e.toString());
            System.exit(-1);
            return;
        }

        // setup the ssh server with some defaults and the options given on the command line.
        log.info("starting REX server");
        SshServer server = SshServer.setUpDefaultServer();

        File pubKeyFile = opts.valueOf(OPT_PUBKEYS);
        if (!pubKeyFile.isFile()) {
            throw new IllegalArgumentException("argument not a file: " + pubKeyFile);
        }
        PubKeyAuthenticator auth = new PubKeyAuthenticator(opts.valueOf(OPT_USER), pubKeyFile);
        server.setPublickeyAuthenticator(auth);

        server.setKeyPairProvider(new SimpleGeneratorHostKeyProvider(opts.valueOf(OPT_HOSTKEY)
                .getAbsolutePath()));

        Map<String, String> props = server.getProperties();
        props.put(SshServer.AUTH_METHODS, "publickey");
        props.put(SshServer.SERVER_IDENTIFICATION, "REX-Service");
        props.put(SshServer.IDLE_TIMEOUT, Integer.toString(IDLE_MASTER_TIMEOUT));

        server.setShellFactory(new RexShellFactory());
        server.setCommandFactory(COMMAND_FACTORY);

        server.setPort(opts.valueOf(OPT_PORT));
        server.start();
    }

    public static RexCommandFactory getCommandFactory() {
        return COMMAND_FACTORY;
    }
}
