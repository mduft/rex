/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpecBuilder;
import at.mduft.rex.util.CrNlHelpFormatter;
import at.mduft.rex.util.HelpAppender;

/**
 * Command that will convert paths from client to server format and vice versa.
 */
public class PathConvCommand extends SimpleCommand {

    private static final OptionParser PARSER;
    private final OptionSet opts;
    private static final ArgumentAcceptingOptionSpec<String> OPT_ROOT;
    private static final ArgumentAcceptingOptionSpec<String> OPT_TOSERVER;
    private static final ArgumentAcceptingOptionSpec<String> OPT_TOCLIENT;
    private static final OptionSpecBuilder OPT_CHECKSERVER;

    static {
        PARSER = new OptionParser();
        PARSER.formatHelpWith(CrNlHelpFormatter.INSTANCE);

        OPT_ROOT = PARSER
                .acceptsAll(Arrays.asList("root", "r"),
                        "mount point of the shared filesystem on the client").withRequiredArg()
                .describedAs("root").required();
        OPT_TOSERVER = PARSER
                .acceptsAll(Arrays.asList("to-server", "s"),
                        "paths to be converted to server format").withRequiredArg()
                .describedAs("client-path,...").withValuesSeparatedBy(',');
        OPT_CHECKSERVER = PARSER.acceptsAll(Arrays.asList("check-exists", "e"),
                "check whether the path on the server exists. prefixes result with '!' if not.");
        OPT_TOCLIENT = PARSER
                .acceptsAll(Arrays.asList("to-client", "c"),
                        "paths to be converted to client format").withRequiredArg()
                .describedAs("server-path,...").withValuesSeparatedBy(',');
    }

    /**
     * Creates a new {@link PathConvCommand} with the given raw arguments from the client.
     * 
     * @param arguments
     *            the raw arguments containing the operation to perform.
     */
    public PathConvCommand(String[] arguments) {
        this.opts = PARSER.parse(arguments);
    }

    @Override
    public Integer call() throws Exception {
        ArgumentProcessor proc = new ArgumentProcessor(opts.valueOf(OPT_ROOT));
        try (PrintWriter wr = new PrintWriter(out)) {
            for (String x : opts.valuesOf(OPT_TOSERVER)) {
                String path = proc.transformPath(x, true);
                if (opts.has(OPT_CHECKSERVER)) {
                    File f = new File(path);
                    if (!f.exists()) {
                        wr.print('!');
                    }
                }
                wr.println(path);
            }
            for (String x : opts.valuesOf(OPT_TOCLIENT)) {
                wr.println(proc.transformPath(x, false));
            }
        }

        return 0;
    }

    @HelpAppender
    public static void appendHelp(StringBuilder builder) throws IOException {
        try (StringWriter wr = new StringWriter()) {
            PARSER.printHelpOn(wr);
            builder.append(wr.toString());
        }
    }

}
