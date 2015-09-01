package edu.nyu.jet.ice.views.cli;

import org.apache.commons.cli.*;


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
        Option corpus = Option.builder().longOpt("corpus").hasArg().argName("corpusName")
                .desc("Specify name of the corpus").required().build();
        options.addOption(corpus);

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            String[] arguments = cmd.getArgs();
            String action  = arguments[0];
            if (action.equals("addCorpus")) {

            }
            else if (action.equals("setBackground")) {

            }
            else if (action.equals("preprocess")) {

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

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "IceCLI [ACTION=addCorpus|setBackground|preprocess|entityIndex|pathExtraction] [OPTIONS]",
                options);
    }

}
