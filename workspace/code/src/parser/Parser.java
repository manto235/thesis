package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.JsonParseException;

import edu.umass.cs.benchlab.har.*;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
//import edu.umass.cs.benchlab.har.tools.HarFileWriter;

public class Parser {

	private static boolean showDebug;
	private static boolean showTrackers;
	private static boolean showStats;
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
	private static RegexGhostery regexGhostery;
	private static Map<String, Integer> trackersStats;
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

	public static void launchParser(String directoryName, boolean debug, boolean trackers, boolean stats) {
		showDebug = debug;
		showTrackers = trackers;
		showStats = stats;
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
			if(showDebug) ioe.printStackTrace();
		}

		// Load the regex from Ghostery
		regexGhostery = new RegexGhostery();
		if(!regexGhostery.isSuccess()) {
			logMessage("Error: the list of trackers could not be retrieved.", true);
			closeLogFile();
			System.exit(1);
		}
		logMessage("Info: the database of trackers has been loaded from Ghostery.", true);
		logMessage("                        Version of bugs: " + regexGhostery.getBugsVersion(), false);
		logMessage("                        Number of elements: " + regexGhostery.getRegex().size(), false);

		// Initialize the Map for the trackers statistics
		trackersStats = new HashMap<String, Integer>();
		for(String trackerName : regexGhostery.getRegex().values()) {
			trackersStats.put(trackerName, 0);
		}

		// Load the list of files
		ArrayList<File> filesList = loadFiles(directoryName);

		int totalTrackersCount = 0;
		for (File file : filesList) {
			logMessage("Info: parsing " + file.getName() + "...", true);
			totalTrackersCount += parseHARfile(file);
		}

		logMessage("Info: the parsing of the files is done!", true);
		logMessage("                        Total number of trackers: " + totalTrackersCount, false);

		if(showStats) showStats();

		logMessage("", false);
		logMessage("----- Summary -----", false);
		if(filesList.size() > 1) {
			logMessage("|  " + filesList.size() + " files", false);
		}
		else {
			logMessage("|  " + filesList.size() + " file", false);
		}
		if(countFails > 1) {
			logMessage("|  " + countFails + " fails", false);
		}
		else {
			logMessage("|  " + countFails + " fail", false);
		}
		if(countSuccesses > 1) {
			logMessage("|  " + countSuccesses + " successes", false);
		}
		else {
			logMessage("|  " + countSuccesses + " success", false);
		}
		logMessage("-------------------", false);

		closeLogFile();
	}

	public static int parseHARfile(File file) {
		try {
			HarFileReader r = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = r.readHarFile(file, warnings);
			for (HarWarning w : warnings)
				logMessage("             >>>>>>>>>> Warning: " + w, false);

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
			return trackersFound;
		}
		catch (JsonParseException e)
		{
			if(showDebug) e.printStackTrace();
			logMessage("             >>>>>>>>>> Parsing error : " + file.getName(), false);
			countFails++;
			return 0;
		}
		catch (IOException e)
		{
			if(showDebug) e.printStackTrace();
			logMessage("             >>>>>>>>>> IO exception : " + file.getName(), false);
			countFails++;
			return 0;
		}
	}



	public static int checkRegexGhostery(String url) {
		int trackersFound = 0;
		Map<String, String> regex = regexGhostery.getRegex();
		for(String singleRegex : regex.keySet()) {
			Pattern pattern = Pattern.compile(singleRegex);
			Matcher matcher = pattern.matcher(url);
			if(matcher.find()) {
				int trackerCount = trackersStats.get(regex.get(singleRegex));
				trackersStats.put(regex.get(singleRegex), trackerCount+1);
				if(showTrackers) {
					logMessage("    Tracker found: " + url + "\n"
							+ "        " + singleRegex + " from " + regex.get(singleRegex), false);
				}
				trackersFound++;
			}
		}
		return trackersFound;
	}

	public static void showStats() {
		logMessage("", false);
		logMessage("----- Statistics -----", false);

		logMessage("> Number of trackers entities: " + trackersStats.size(), false);

		List<Map.Entry<String, Integer>> entries = new LinkedList<Map.Entry<String, Integer>>(trackersStats.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
				return -o1.getValue().compareTo(o2.getValue());
			}
		});

		Map<String, Integer> sortedTrackersStats = new LinkedHashMap<String, Integer>();
		for(Map.Entry<String, Integer> entry: entries){
			sortedTrackersStats.put(entry.getKey(), entry.getValue());
		}

		for(String name : sortedTrackersStats.keySet()) {
			int trackerCount;
			if((trackerCount = trackersStats.get(name)) != 0)
				logMessage(name + ": " + trackerCount, false);
		}
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
			if(showDebug) ioe.printStackTrace();
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
			if(showDebug) ioe.printStackTrace();
		}
	}
}
