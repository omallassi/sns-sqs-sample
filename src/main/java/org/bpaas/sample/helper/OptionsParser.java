package org.bpaas.sample.helper;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.ExampleMode;


public class OptionsParser {
    private static OptionsParser ourInstance = new OptionsParser();

    public static OptionsParser getInstance() {
        return ourInstance;
    }

    private OptionsParser() {
    }

    private static CmdLineParser parser;

    public static void parseCommandLine(Object obj, String[] args) {
        parser = new CmdLineParser(obj);

        parser.setUsageWidth(80);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // print the list of available options
            parser.printUsage(System.err);
            System.err.println();

            System.exit(1);
        }
    }

    public static void printHelp() {
        parser.printUsage(System.err);
        System.err.println();
        return;
    }
}
