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
		options.addOption("mode", true, "c (crawler), p (parser) or cp (crawler & parser)");
		options.addOption("dir", true, "directory containing the files generated (crawler mode) or the files to parse (parser mode)");
		options.addOption("port", true, "port of the proxy");
		options.addOption("file", true, "path to the top-1m.csv file (Alexa Top file)");
		options.addOption("bi", true, "begin index in the Alexa Top file");
		options.addOption("ei", true, "end index in the Alexa Top file");
		options.addOption("h", false, "help");

		CommandLineParser parser = new PosixParser();
		try {
			cmd = parser.parse( options, args);
			if(cmd.hasOption("h")) {
				String help = "Launch with the following arguments: -mode [mode] -dir [directory] -port [port] -file [file] -bi [begin index] -ei [end index]\n"
						+ "If the mode is \"crawler\" or \"crawler & parser\", add the following arguments: port, file, bi and ei.\n"
						+ "The indexes correspond to the range of websites to visit in the Alexa Top file.\n";
				System.out.println(help);
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp("java -jar Code.jar", options);
			}
			// Required: mode
			else if(!cmd.hasOption("mode")) {
				System.out.println("Mode is required!\nLaunch with -h for help");
			}
			// Required: directory
			else if(!cmd.hasOption("dir")) {
				System.out.println("Directory is required!");
			}
			else {
				String directory = cmd.getOptionValue("dir");
				//String directory;
				/*try {
					directory = parseDirectory(cmd.getOptionValue("dir"));
				} catch (Exception e1) {
					System.out.println("An error occurred. Check the directory argument.");
					return;
				}*/
				String mode = cmd.getOptionValue("mode");

				if(mode.equals("p")) {
					System.out.println("Launching parser...\n"
							+ "directory: " + cmd.getOptionValue("dir"));
					Parser.launchParser(directory);
				}

				else if(mode.equals("c") || mode.equals("cp")) {
					if(checkArgsCrawler(cmd.hasOption("port"), cmd.hasOption("file"), cmd.hasOption("bi"), cmd.hasOption("ei"))) {
						try {
							int port = parsePort(cmd.getOptionValue("port"));
							String file = parseFile(cmd.getOptionValue("file"));
							int beginIndex = parseBeginIndex(cmd.getOptionValue("bi"));
							int endIndex = parseEndIndex(cmd.getOptionValue("ei"));

							System.out.println("Launching crawler...\n"
									+ "directory: " + cmd.getOptionValue("dir") + "\n"
									+ "port: " + port + ", file : " + file
									+ ", begin index: " + beginIndex + ", end index: " + endIndex);
							Crawler.launchCrawler(directory, port, beginIndex, endIndex);

							if(mode.equals("cp")) {
								System.out.println("Launching parser...\n"
										+ "directory: " + cmd.getOptionValue("dir"));
								Parser.launchParser(directory);
							}
						} catch (Exception e) {
							System.out.println("Cannot launch the crawler. Check the arguments.");
							return;
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
			System.out.println("Directory not found!");
			throw new Exception();
		}
		return path;
	}

	public static boolean checkArgsCrawler(boolean port, boolean f, boolean b, boolean e) {
		String message = "The following arguments are missing:\n";
		if(!port) message += " - port of the proxy\n";
		if(!f) message += " - path to the top-1m.csv file\n";
		if(!b) message += " - begin index\n";
		if(!e) message += " - end index\n";

		boolean check = port & f & b & e;
		if(!check) System.out.print(message);
		return check;
	}

	/**
	 * Parses the port received as argument.
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
			System.out.println("File not found!");
			throw new Exception();
		}
		else {
			return path;
		}
	}

	/**
	 * Parses the begin index received as argument.
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
}