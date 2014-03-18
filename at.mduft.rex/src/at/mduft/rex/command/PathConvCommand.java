/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.command;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import at.mduft.rex.util.CrNlHelpFormatter;

public class PathConvCommand extends SimpleCommand {

    private static final OptionParser PARSER;
    private final OptionSet opts;
    private static ArgumentAcceptingOptionSpec<String> OPT_ROOT;
    private static ArgumentAcceptingOptionSpec<String> OPT_TOSERVER;
    private static ArgumentAcceptingOptionSpec<String> OPT_TOCLIENT;

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
        OPT_TOCLIENT = PARSER
                .acceptsAll(Arrays.asList("to-client", "c"),
                        "paths to be converted to client format").withRequiredArg()
                .describedAs("server-path,...").withValuesSeparatedBy(',');
    }

    public PathConvCommand(String[] arguments) {
        this.opts = PARSER.parse(arguments);
    }

    @Override
    public Integer call() throws Exception {
        ArgumentProcessor proc = new ArgumentProcessor(opts.valueOf(OPT_ROOT), null);
        try (PrintWriter wr = new PrintWriter(out)) {
            for (String x : opts.valuesOf(OPT_TOSERVER)) {
                wr.println(proc.transformPath(x, true));
            }
            for (String x : opts.valuesOf(OPT_TOCLIENT)) {
                wr.println(proc.transformPath(x, false));
            }
        }

        return 0;
    }

    public static void appendHelp(StringBuilder builder) throws IOException {
        try (StringWriter wr = new StringWriter()) {
            PARSER.printHelpOn(wr);
            builder.append(wr.toString());
        }
    }

}
