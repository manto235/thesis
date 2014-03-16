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
		options.addOption("port", true, "required for crawler: port of the proxy");
		options.addOption("file", true, "required for crawler: path to the Alexa Top file (top-1m.csv)");
		options.addOption("bi", true, "required for crawler: begin index in the Alexa Top file");
		options.addOption("ei", true, "required for crawler: end index in the Alexa Top file");
		options.addOption("a", true, "optional for crawler: number of attempts per website");
		options.addOption("debug", false, "debug");
		options.addOption("h", false, "help");

		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse(options, args);
			// Help
			if(cmd.hasOption("h")) {
				String help = "Launch with the following arguments: -mode [mode] -dir [directory] -port [port] -file [file] -bi [begin index] -ei [end index]\n"
						+ "If the mode is \"crawler\" or \"crawler & parser\", add the following arguments: port, file, bi and ei.\n"
						+ "The indexes correspond to the range of websites to visit in the Alexa Top file.\n";
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
				System.out.println("Directory is required!");
			}
			else {
				String mode = cmd.getOptionValue("mode");
				String directory = cmd.getOptionValue("dir");

				// Mode: parser
				if(mode.equals("p")) {
					System.out.println("Launching parser...\n"
							+ "directory: " + cmd.getOptionValue("dir"));
					Parser.launchParser(directory);
				}
				// Mode: crawler or crawler & parser
				else if(mode.equals("c") || mode.equals("cp")) {
					if(checkRequiredArgsCrawler(cmd.hasOption("port"), cmd.hasOption("file"), cmd.hasOption("bi"), cmd.hasOption("ei"))) {
						try {
							int port = parsePort(cmd.getOptionValue("port"));
							String file = parseFile(cmd.getOptionValue("file"));
							int beginIndex = parseBeginIndex(cmd.getOptionValue("bi"));
							int endIndex = parseEndIndex(cmd.getOptionValue("ei"));
							int attempts = 1; // 1 by default
							if(cmd.hasOption("a")) {
								attempts = parseAttempts(cmd.getOptionValue("a"));
							}

							Crawler.launchCrawler(directory, port, file, beginIndex, endIndex, attempts, cmd.hasOption("debug"));

							// Mode: crawler & parser
							if(mode.equals("cp")) {
								System.out.println("Launching parser...\n"
										+ "directory: " + cmd.getOptionValue("dir"));
								Parser.launchParser(directory);
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
	 * @param port
	 * @param file
	 * @param bi
	 * @param ei
	 * @return true if no required argument is missing, false otherwise
	 */
	public static boolean checkRequiredArgsCrawler(boolean port, boolean file, boolean bi, boolean ei) {
		String message = "The following arguments are missing:\n";
		if(!port) message += " - port of the proxy\n";
		if(!file) message += " - path to the top-1m.csv file\n";
		if(!bi) message += " - begin index\n";
		if(!ei) message += " - end index\n";

		boolean check = port & file & bi & ei;
		if(!check) System.out.print(message);
		return check;
	}

	/**
	 * Parses the port received as argument.
	 * If the port is not an integer, a message is printed in the console.
	 *
	 * @param port the port as a String
	 * @return the port as an int
	 * @throws Exception
	 */
	public static int parsePort(String port) throws Exception {
		try {
			return Integer.parseInt(port);
		} catch (Exception e) {
			System.out.println("Port must be an integer!");
			throw new Exception();
		}
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
			System.out.println("File not found! Check your -file argument.");
			throw new Exception();
		}
		else {
			return path;
		}
	}

	/**
	 * Parses the begin index received as argument.
	 * If the index is not an integer, a message is printed in the console.
	 *
	 * @param index the index as a String
	 * @return the index as an int
	 * @throws Exception
	 */
	public static int parseBeginIndex(String index) throws Exception {
		try {
			return Integer.parseInt(index);
		} catch (Exception e) {
			System.out.println("Begin index must be an integer!");
			throw new Exception();
		}
	}

	/**
	 * Parses the end index received as argument.
	 * If the index is not an integer, a message is printed in the console.
	 *
	 * @param index the index as a String
	 * @return the index as an int
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
	 * @return the number of attempts as an int
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
