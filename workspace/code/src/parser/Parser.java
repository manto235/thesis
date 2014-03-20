package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;

import edu.umass.cs.benchlab.har.*;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
//import edu.umass.cs.benchlab.har.tools.HarFileWriter;

public class Parser {

	private static boolean debug;
	private static boolean verbose;
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
	private static ArrayList<String> regexGhostery;
	private static int countFails = 0;
	private static int countSuccesses = 0;

	/**
	 * Loads the files from a directory
	 * 
	 * @param directoryName: the directory containing the files to load
	 * @return an ArrayList<File> containing all the files of the directory
	 */
	public static ArrayList<File> loadFiles(String directoryName) {
		logMessage("Info: loading the files from directory \"" + directoryName + "\"... ", true);
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			logMessage("Error: the directory does not exist!", true);
			closeLogFile();
			System.exit(1);
		}

		File[] files = directory.listFiles();
		ArrayList<File> filesList = new ArrayList<File>();

		for (File file : files) {
			if(file.isFile()) {
				filesList.add(file);
			}
		}

		return filesList;
	}

	public static void launchParser(String directoryName, boolean showDebug, boolean activateVerbose) {
		debug = showDebug;
		verbose = activateVerbose;
		String start = "----------------------------------------\n"
				+ dateFormat.format(new Date()) + " - Launching parser...\n"
				+ "   directory: " + directoryName;
		System.out.println(start);
		try {
			logsFile = new BufferedWriter(new FileWriter(new File("logs_parser.txt"), true));
			logsFile.write(start);
			logsFile.newLine();
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the logs file.\n> Please check your file system permissions.");
			System.out.println("The parser will however continue...");
			if(debug) ioe.printStackTrace();
		}

		// Load the regex from Ghostery
		regexGhostery = new RegexGhostery().getRegex();
		if(regexGhostery.size() == 0) {
			logMessage("Error: cannot find the Ghostery database.", true);
			closeLogFile();
			System.exit(1);
		}

		// Load the list of files
		ArrayList<File> filesList = loadFiles(directoryName);

		for (File file : filesList) {
			logMessage("Info: parsing " + file.getName() + "...", true);
			parseHARfile(file);
		}

		logMessage(" ----- Summary -----", false);
		logMessage("  " + filesList.size() + " files", false);
		logMessage("  " + countFails + " fails", false);
		logMessage("  " + countSuccesses + " successes", false);

		closeLogFile();
	}

	public static void parseHARfile(File file) {
		try {
			HarFileReader r = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = r.readHarFile(file, warnings);
			for (HarWarning w : warnings)
				logMessage("Warning: " + w, true);

			//HarFileWriter w = new HarFileWriter();

			// Access all elements as objects
			//HarBrowser browser = log.getBrowser();
			HarEntries entries = log.getEntries();
			List<HarEntry> entriesList = entries.getEntries();

			int trackersFound = 0;
			for (HarEntry entry : entriesList) {
				//System.out.println(("Entry (request URL) : " + entry.getRequest().getUrl()));
				trackersFound += checkRegexGhostery((entry.getRequest().getUrl()));
				//System.out.println("-- Entry (response) : " + entry.getResponse());
				//System.out.println("> Entry (response CONTENT MIMETYPE) : " + entry.getResponse().getContent().getMimeType());
			}

			logMessage("                        Number of trackers found: " + trackersFound, false);

			// Once you are done manipulating the objects, write back to a file
			//System.out.println("Writing " + fileName + ".parsed");
			//File f2 = new File(fileName + ".parsed");
			//w.writeHarFile(log, f2);
			countSuccesses++;
		}
		catch (JsonParseException e)
		{
			e.printStackTrace();
			if(debug) System.out.println("Parsing error : " + file.getName());
			countFails++;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			if(debug) System.out.println("IO exception : " + file.getName());
			countFails++;
		}
	}



	public static int checkRegexGhostery(String url) {
		int trackersFound = 0;
		for(String singleRegex : regexGhostery) {
			Pattern pattern = Pattern.compile(singleRegex);
			Matcher matcher = pattern.matcher(url);
			if(matcher.find()) {
				if(verbose) {
					logMessage("    Tracker found: " + url + " <= " + singleRegex, false);
				}
				trackersFound++;
			}
		}
		return trackersFound;
	}

	/**
	 * Prints a message in the console and writes a message in the log file.
	 * @param message the message to print and write
	 * @param showTime add the time before the message
	 */
	public static void logMessage(String message, boolean showTime) {
		if(showTime) {
			message = dateFormat.format(new Date()) + " - " + message;
		}
		System.out.println(message);
		try {
			logsFile.write(message);
			logsFile.newLine();
		} catch (IOException ioe) {
			System.out.println("The message was not successfully written in the log file.");
			if(debug) ioe.printStackTrace();
		}
	}

	/**
	 * 	Closes the logs file.
	 *  If a problem occurs, prints a message in the console.
	 */
	public static void closeLogFile() {
		try {
			logsFile.close();
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot close the logs file.\n> It may be corrupted.");
			if(debug) ioe.printStackTrace();
		}
	}
}
