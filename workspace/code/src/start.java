import java.io.File;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import parser.Parser;
import crawler.Crawler;

public class start {
	public static void main (String[] args) {
		CommandLine cmd;
		Options options = new Options();
		// General
		options.addOption("mode", true, "required: c (crawler) or p (parser)");
		options.addOption("dir", true, "required: directory containing the files generated (crawler mode) or the files to parse (parser mode)");
		options.addOption("debug", false, "enable the debug messages");
		options.addOption("h", false, "help");

		// Crawler
		options.addOption("ffprofile", true, "crawler (required): name of the Firefox profile");
		options.addOption("websites", true, "crawler (required): path to the websites file");
		options.addOption("start", true, "crawler (required): start index in the websites file");
		options.addOption("end", true, "crawler (required): end index in the websites file");
		options.addOption("attempts", true, "crawler (optional): number of attempts per website");
		options.addOption("restart", true, "crawler (required): number of websites to visit before restarting Firefox");

		// Parser
		options.addOption("trackers", false, "parser (optional): show all trackers (print a lot)");
		options.addOption("ghostery", true, "crawler (required): number of websites to visit before restarting Firefox");


		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);
			// Help
			if(cmd.hasOption("h")) {
				String help = "Launch with the following arguments: -mode [mode] -dir [directory]\n"
						+ "If the mode uses the crawler, add the following arguments: -ffprofile [profile] -websites [file] -start [index] -end [index]\n"
						+ "The indexes correspond to the range of websites to visit from the websites file.\n";
				System.out.println(help);
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar Code.jar", options);
			}
			// Mode (required)
			else if(!cmd.hasOption("mode")) {
				System.out.println("Mode is required!\nLaunch with -h for help");
			}
			// Directory (required)
			else if(!cmd.hasOption("dir")) {
				System.out.println("Directory is required!\nLaunch with -h for help");
			}
			else {
				String mode = cmd.getOptionValue("mode");
				String directory = cmd.getOptionValue("dir");

				// Mode: parser
				if(mode.equals("p")) {
					if(checkRequiredArgsParser(cmd.hasOption("ghostery"))) {
						try {
							// Check if the directory exists
							if(!new File(directory).isDirectory()) {
								System.out.println("Directory not found! Check your -dir argument.");
								System.exit(1);
							}
							else {
								String ghostery = parseFile(cmd.getOptionValue("ghostery"), "ghostery");
								Parser.launchParser(directory, cmd.hasOption("debug"), cmd.hasOption("trackers"), ghostery);
							}
						} catch (Exception e) {
							System.out.println("An error occurred with the parser.");
							if(cmd.hasOption("debug")) e.printStackTrace();
							System.exit(1);
						}
					}
				}
				// Mode: crawler
				else if(mode.equals("c")) {
					if(checkRequiredArgsCrawler(cmd.hasOption("ffprofile"), cmd.hasOption("websites"), cmd.hasOption("start"), cmd.hasOption("end"), cmd.hasOption("restart"))) {
						try {
							String websites = parseFile(cmd.getOptionValue("websites"), "websites");
							int startIndex = parseStartIndex(cmd.getOptionValue("start"));
							int endIndex = parseEndIndex(cmd.getOptionValue("end"));
							int restart = parseRestartValue(cmd.getOptionValue("restart"));
							int attempts = 1; // 1 by default
							if(cmd.hasOption("attempts")) {
								attempts = parseAttempts(cmd.getOptionValue("attempts"));
							}

							Crawler.launchCrawler(directory, cmd.getOptionValue("ffprofile"), websites, startIndex, endIndex, attempts, cmd.hasOption("debug"), restart);
						} catch (Exception e) {
							System.out.println("An error occurred with the crawler.");
							if(cmd.hasOption("debug")) e.printStackTrace();
							System.exit(1);
						}
					}

				}
				else {
					System.out.println("This mode does not exist.\nLaunch with -h for help");
				}
			}

		} catch (ParseException e) {
			System.out.println("Arguments not recognized!");
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("java -jar Code.jar", options);
		}
	}

	/**
	 * Checks if required arguments are missing for the crawler mode.
	 * Prints the list of missing arguments in the console.
	 *
	 * @param ffprofile
	 * @param websites
	 * @param start
	 * @param end
	 * @return true if no required argument is missing, false otherwise
	 */
	public static boolean checkRequiredArgsCrawler(boolean ffprofile, boolean websites, boolean start, boolean end, boolean restart) {
		String message = "The following arguments are missing:\n";
		if(!ffprofile) message += " - name of the Firefox profile\n";
		if(!websites) message += " - path to the websites file\n";
		if(!start) message += " - start index\n";
		if(!end) message += " - end index\n";
		if(!restart) message += " - websites visits before Firefox restart\n";

		boolean check = ffprofile & websites & start & end & restart;
		if(!check) System.out.print(message);
		return check;
	}

	/**
	 * Checks if required arguments are missing for the parser mode.
	 * Prints the list of missing arguments in the console.
	 *
	 * @param ghostery
	 * @return true if no required argument is missing, false otherwise
	 */
	public static boolean checkRequiredArgsParser(boolean ghostery) {
		String message = "The following arguments are missing:\n";
		if(!ghostery) message += " - path to the Ghostery file\n";

		boolean check = ghostery;
		if(!check) System.out.print(message);
		return check;
	}

	/**
	 * Checks if the path corresponds to a file and if it exists.
	 * If the file does not exist, a message is printed in the console.
	 *
	 * @param path the path of the file
	 * @param type the type of the file
	 * @return the path if the file exists, throws an exception otherwise
	 * @throws Exception 
	 */
	public static String parseFile(String path, String type) throws Exception {
		File file = new File(path);
		if(!file.isFile()) {
			System.out.println("File not found! Check your -" + type + " argument.");
			throw new Exception();
		}
		else {
			return path;
		}
	}

	/**
	 * Parses the start index received as argument.
	 * If the index is not an integer, a message is printed in the console.
	 *
	 * @param index the index as a String
	 * @return the index as an Integer
	 * @throws Exception
	 */
	public static int parseStartIndex(String index) throws Exception {
		try {
			return Integer.parseInt(index);
		} catch (Exception e) {
			System.out.println("Start index must be an integer!");
			throw new Exception();
		}
	}

	/**
	 * Parses the end index received as argument.
	 * If the index is not an integer, a message is printed in the console.
	 *
	 * @param index the index as a String
	 * @return the index as an Integer
	 * @throws Exception
	 */
	public static int parseEndIndex(String index) throws Exception {
		try {
			return Integer.parseInt(index);
		} catch (Exception e) {
			System.out.println("End index must be an integer!");
			throw new Exception();
		}
	}

	/**
	 * Parses the number of attempts received as argument.
	 * If the number of attempts is not an integer, a message is printed in the console.
	 *
	 * @param attempts the number of attempts as a String
	 * @return the number of attempts as an Integer
	 * @throws Exception
	 */
	public static int parseAttempts(String attempts) throws Exception {
		try {
			return Integer.parseInt(attempts);
		} catch (Exception e) {
			System.out.println("Number of attempts must be an integer!");
			throw new Exception();
		}
	}

	/**
	 * Parses the restart value received as argument.
	 * If the restart value is not an integer, a message is printed in the console.
	 *
	 * @param value the restart value as a String
	 * @return the restart value as an Integer
	 * @throws Exception
	 */
	public static int parseRestartValue(String value) throws Exception {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			System.out.println("Restart value must be an integer!");
			throw new Exception();
		}
	}
}
