package edu.nyu.jet.ice.views.cli;

import edu.nyu.jet.ice.controllers.Nice;
import org.apache.commons.cli.*;

import java.io.File;
import java.util.Properties;


/**
 * Command line interface for running Ice processing tasks.
 *
 * @author yhe
 * @version 1.0
 */
public class IceCLI {

    public static void main(String[] args) {
        // create Options object
        Options options = new Options();
        Option inputDir = Option.builder().longOpt("inputDir").hasArg().argName("inputDirNae")
                .desc("Location of new corpus").build();
        Option background = Option.builder().longOpt("background").hasArg().argName("backgroundCorpusName")
                .desc("Name of the background corpus").build();
        options.addOption(inputDir);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String[] arguments = cmd.getArgs();
            if (arguments.length < 2) {
                printHelp(options);
                System.exit(-1);
            }
            String action  = arguments[0];
            String corpusName  = arguments[1];

            if (action.equals("preprocess")) {
                String inputDirName = cmd.getOptionValue("inputDir");
                if (inputDirName == null) {
                    printHelp(options);
                    System.err.println("-inputDir should be set for the preprocess action.");
                    System.exit(-1);
                }
                File inputDirFile = new File(inputDirName);
                if (inputDirFile.exists() && inputDirFile.isDirectory()) {
                    init();
                }
                else {
                    printHelp(options);
                    System.err.println("-inputDir should specify a valid directory.");
                    System.exit(-1);
                }

            }
            else if (action.equals("setBackground")) {

            }
            else if (action.equals("entityIndex")) {

            }
            else if (action.equals("pathExtraction")) {

            }
            else {
                printHelp(options);
                System.err.println("Unsupported action: " + action);
            }
        }
        catch (MissingOptionException e) {
            printHelp(options);
            System.err.println(e.getMessage());
            System.exit(-1);
        }
        catch (ParseException e) {
            printHelp(options);
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public static void init() {
        Nice.printCover();
        Properties iceProperties = Nice.loadIceProperties();
        Nice.initIce();
        Nice.loadPathMatcher(iceProperties);
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "IceCLI [ACTION=preprocess|setBackground|entityIndex|pathExtraction] [OPTIONS]",
                options);
    }

}
