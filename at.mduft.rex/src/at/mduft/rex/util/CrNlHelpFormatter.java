/*
 * Copyright (c) Salomon Automation GmbH
 */
package at.mduft.rex.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import at.mduft.rex.command.DefaultCommand;

/**
 * Special help formatter used by the {@link DefaultCommand} to print out help for all commands that
 * wish to do so by implementing a static method {@code appendHelp(StringBuilder)}.
 */
public class CrNlHelpFormatter implements HelpFormatter {

    /** default instance - may be reused by different parsers */
    public static final HelpFormatter INSTANCE = new CrNlHelpFormatter();

    private static final String NL = "\r\n";
    private static final String IND = "\t  ";

    @Override
    public String format(Map<String, ? extends OptionDescriptor> options) {
        List<List<String>> table = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        headers.add("Option");
        headers.add("Req.");
        headers.add("Default");
        headers.add("Description");

        table.add(headers);

        // create HashSet to avoid duplicates, as options might have synonyms.
        for (OptionDescriptor opt : new HashSet<>(options.values())) {
            if (opt.representsNonOptions()) {
                continue;
            }

            List<String> line = new ArrayList<>();
            StringBuilder option = new StringBuilder();
            boolean first = true;
            for (String synonym : opt.options()) {
                if (!first) {
                    option.append(", ");
                }
                first = false;

                option.append('-');
                if (synonym.length() > 1) {
                    option.append('-');
                }
                option.append(synonym);
            }
            if (opt.acceptsArguments()) {
                char x = opt.requiresArgument() ? '<' : '[';
                char y = opt.requiresArgument() ? '>' : ']';
                option.append(' ').append(x);
                String type = opt.argumentTypeIndicator();
                if (type != null && !type.isEmpty()) {
                    option.append(type).append(": ");
                }
                String desc = opt.argumentDescription();
                if (desc == null || desc.isEmpty()) {
                    option.append("arg");
                } else {
                    option.append(desc);
                }
                option.append(y);
            }

            line.add(option.toString());
            line.add(opt.isRequired() ? "*" : "");
            line.add(opt.defaultValues().toString());
            line.add(opt.description());
            table.add(line);
        }

        return formatTable(table);
    }

    private String formatTable(List<List<String>> table) {
        List<String> headers = table.get(0);
        int sizes[] = new int[headers.size()];
        for (int i = 0; i < sizes.length; ++i) {
            for (List<String> l : table) {
                int length = l.get(i).length();
                if (length + 1 > sizes[i]) {
                    sizes[i] = length + 1;
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        builder.append(IND).append(formatCells(headers, sizes)).append(NL);
        builder.append(IND).append(formatSeparator(sizes)).append(NL);
        for (int i = 1; i < table.size(); ++i) {
            builder.append(IND).append(formatCells(table.get(i), sizes)).append(NL);
        }

        return builder.toString();
    }

    private String formatSeparator(int[] sizes) {
        int all = 0;
        for (int x : sizes) {
            all += x;
        }

        char sep[] = new char[all];
        Arrays.fill(sep, '=');
        return new String(sep);
    }

    private String formatCells(List<String> lineData, int[] sizes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sizes.length; ++i) {
            builder.append(String.format("%-" + sizes[i] + "s", lineData.get(i)));
        }
        return builder.toString();
    }

}
