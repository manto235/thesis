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
		options.addOption("mode", true, "required: c (crawler), p (parser) or cp (crawler & parser)");
		options.addOption("dir", true, "required: directory containing the files generated (crawler mode) or the files to parse (parser mode)");
		options.addOption("ffprofile", true, "crawler (required): name of the Firefox profile");
		options.addOption("websites", true, "crawler (required): path to the websites file");
		options.addOption("start", true, "crawler (required): begin index in the websites file");
		options.addOption("end", true, "crawler (required): end index in the websites file");
		options.addOption("a", true, "crawler (optional): number of attempts per website");
		options.addOption("trackers", false, "parser (optional): show all trackers (print a lot)");
		options.addOption("stats", false, "parser (optional): show stats of trackers entities");
		options.addOption("debug", false, "enable the debug messages");
		options.addOption("h", false, "help");

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
					Parser.launchParser(directory, cmd.hasOption("debug"), cmd.hasOption("trackers"), cmd.hasOption("stats"));
				}
				// Mode: crawler or crawler & parser
				else if(mode.equals("c") || mode.equals("cp")) {
					if(checkRequiredArgsCrawler(cmd.hasOption("ffprofile"), cmd.hasOption("websites"), cmd.hasOption("start"), cmd.hasOption("end"))) {
						try {
							String websites = parseFile(cmd.getOptionValue("websites"));
							int beginIndex = parseBeginIndex(cmd.getOptionValue("start"));
							int endIndex = parseEndIndex(cmd.getOptionValue("end"));
							int attempts = 1; // 1 by default
							if(cmd.hasOption("a")) {
								attempts = parseAttempts(cmd.getOptionValue("a"));
							}

							Crawler.launchCrawler(directory, cmd.getOptionValue("ffprofile"), websites, beginIndex, endIndex, attempts, cmd.hasOption("debug"));

							// Mode: crawler & parser
							if(mode.equals("cp")) {
								Parser.launchParser(directory, cmd.hasOption("debug"),  cmd.hasOption("trackers"), cmd.hasOption("stats"));
							}
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
			System.out.println("Arguments not recognized!\n"
					+ "Launch with -h for help");
		}
	}

	/**
	 * Checks if the path corresponds to a directory and if it exists.
	 * If the directory does not exist, a message is printed in the console.
	 *
	 * @param path the path to check
	 * @return the path if the directory exists, throws an exception otherwise
	 * @throws Exception 
	 */
	public static String parseDirectory(String path) throws Exception {
		File directory = new File(path);
		if(!directory.isDirectory()) {
			System.out.println("Directory not found! Check your -dir argument.");
			throw new Exception();
		}
		return path;
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
	public static boolean checkRequiredArgsCrawler(boolean ffprofile, boolean websites, boolean start, boolean end) {
		String message = "The following arguments are missing:\n";
		if(!ffprofile) message += " - name of the Firefox profile\n";
		if(!websites) message += " - path to the websites file\n";
		if(!start) message += " - start index\n";
		if(!end) message += " - end index\n";

		boolean check = ffprofile & websites & start & end;
		if(!check) System.out.print(message);
		return check;
	}

	/**
	 * Checks if the path corresponds to a file and if it exists.
	 * If the file does not exist, a message is printed in the console.
	 *
	 * @param path the path to check
	 * @return the path if the file exists, throws an exception otherwise
	 * @throws Exception 
	 */
	public static String parseFile(String path) throws Exception {
		File file = new File(path);
		if(!file.isFile()) {
			System.out.println("File not found! Check your -websites argument.");
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
	public static int parseBeginIndex(String index) throws Exception {
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
}
