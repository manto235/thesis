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
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
	private static RegexGhostery regexGhostery;
	private static Map<String, Integer> trackersStats;
	private static Map<String, Integer> websitesStats;
	private static int countSuccesses = 0;
	private static ArrayList<String> filesFailed = new ArrayList<String>();

	public static void launchParser(String directoryName, boolean debug, boolean trackers) {
		showDebug = debug;
		showTrackers = trackers;
		String start = "----------------------------------------\n"
				+ dateFormat.format(new Date()) + " - Launching parser...\n"
				+ "   directory: " + directoryName;
		System.out.println(start);
		try {
			File logParser = new File(directoryName+"/logs/log_parser.txt");
			File logsDirectory = logParser.getParentFile();
			if(!logsDirectory.isDirectory()) {
				if(!logsDirectory.mkdirs()) {
					System.out.println(dateFormat.format(new Date()) + " - Error: cannot create the logs folder.\n"
							+ "> Please check your file system permissions.");
				}
			}
			logsFile = new BufferedWriter(new FileWriter(logParser, true));
			logsFile.write(start);
			logsFile.newLine();
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the logs file.\n"
					+ "> Please check your file system permissions.");
			// TODO stop program if error
			System.out.println("The parser will however continue...");
			if(showDebug) ioe.printStackTrace();
		}

		// Load the regex from Ghostery
		regexGhostery = new RegexGhostery();
		if(!regexGhostery.isSuccess()) {
			logMessage("Error: the list of trackers could not be retrieved.", 1);
			closeLogFile();
			System.exit(1);
		}
		logMessage("Info: the database of trackers has been loaded from Ghostery.", 1);
		logMessage("Version of bugs: " + regexGhostery.getBugsVersion(), 2);
		logMessage("Number of elements: " + regexGhostery.getRegex().size(), 2);

		// Initialize the Map for the trackers statistics
		trackersStats = new HashMap<String, Integer>();
		for(String trackerName : regexGhostery.getRegex().values()) {
			trackersStats.put(trackerName, 0);
		}

		// Initialize the Map for the websites statistics
		websitesStats = new HashMap<String, Integer>();

		// Load the list of files
		ArrayList<File> filesList = loadFiles(directoryName);

		int totalTrackersCount = 0;
		for (File file : filesList) {
			logMessage("Parsing " + file.getName() + "...", 1);
			totalTrackersCount += parseHARfile(file);
		}

		logMessage("Info: the parsing of the files is done!", 1);
		logMessage("Total number of trackers: " + totalTrackersCount, 2);

		// Stats
		computeStats(directoryName);

		// Summary
		logMessage("", 0);
		logMessage("----- Summary -----", 0);
		if(filesList.size() > 1) {
			logMessage("> " + filesList.size() + " files", 0);
		}
		else {
			logMessage("> " + filesList.size() + " file", 0);
		}
		if(filesFailed.size() > 1) {
			logMessage(filesFailed.size() + " fails", 0);
		}
		else {
			logMessage(filesFailed.size() + " fail", 0);
		}
		if(countSuccesses > 1) {
			logMessage(countSuccesses + " successes", 0);
		}
		else {
			logMessage(countSuccesses + " success", 0);
		}

		// Fails
		if(filesFailed.size() > 0) {
			logMessage("", 0);
			logMessage("----- Fails -----", 0);
			for(String fileFailed : filesFailed) {
				logMessage(fileFailed, 0);
			}
		}

		closeLogFile();
	}

	/**
	 * Loads the files from a directory
	 *
	 * @param directoryName: the directory containing the files to load
	 * @return an ArrayList<File> containing all the files of the directory
	 */
	public static ArrayList<File> loadFiles(String directoryName) {
		logMessage("Info: loading the files from directory \"" + directoryName + "\"... ", 1);
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			logMessage("Error: the directory does not exist!", 1);
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

	/**
	 * Called for each website
	 * @param file
	 * @return
	 */
	public static int parseHARfile(File file) {
		try {
			String fileName = file.getName();
			HarFileReader r = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = r.readHarFile(file, warnings);
			for (HarWarning w : warnings)
				logMessage("Warning: " + w, 3);

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

			websitesStats.put(fileName, trackersFound);
			logMessage("Number of trackers found: " + trackersFound, 2);

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
			logMessage("Parsing error : " + file.getName(), 3);
			filesFailed.add(file.getName());
			return 0;
		}
		catch (IOException e)
		{
			if(showDebug) e.printStackTrace();
			logMessage("IO exception : " + file.getName(), 3);
			filesFailed.add(file.getName());
			return 0;
		}
	}

	/**
	 * Called for each URL
	 * @param url
	 * @return
	 */
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
							+ "        " + singleRegex + " from " + regex.get(singleRegex), 0);
				}
				trackersFound++;
			}
		}
		return trackersFound;
	}

	public static void computeStats(String directoryName) {
		try {
			// TRACKERS
			BufferedWriter trackersStatsFile = new BufferedWriter(new FileWriter(new File(directoryName+"/logs/stats_trackers.csv"), false));

			//statsTrackers.write("----- Statistics of trackers -----");
			//statsTrackers.newLine();
			//statsTrackers.write("> Number of trackers entities: " + trackersStats.size());
			//statsTrackers.newLine();

			List<Map.Entry<String, Integer>> trackersEntries = new LinkedList<Map.Entry<String, Integer>>(trackersStats.entrySet());
			Collections.sort(trackersEntries, new Comparator<Map.Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});

			Map<String, Integer> sortedTrackersStats = new LinkedHashMap<String, Integer>();
			for(Map.Entry<String, Integer> entry: trackersEntries){
				sortedTrackersStats.put(entry.getKey(), entry.getValue());
			}

			for(String name : sortedTrackersStats.keySet()) {
				int trackerCount = trackersStats.get(name);
				if(trackerCount != 0) {
					trackersStatsFile.write(name + "," + trackerCount);
					trackersStatsFile.newLine();
				}
			}
			trackersStatsFile.close();

			// WEBSITES
			BufferedWriter websitesStatsFile = new BufferedWriter(new FileWriter(new File(directoryName+"/logs/stats_websites.csv"), false));

			List<Map.Entry<String, Integer>> websitesEntries = new LinkedList<Map.Entry<String, Integer>>(websitesStats.entrySet());
			Collections.sort(websitesEntries, new Comparator<Map.Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});

			Map<String, Integer> sortedWebsitesStats = new LinkedHashMap<String, Integer>();
			for(Map.Entry<String, Integer> entry: websitesEntries){
				sortedWebsitesStats.put(entry.getKey(), entry.getValue());
			}

			for(String name : sortedWebsitesStats.keySet()) {
				int trackerCount = websitesStats.get(name);
				if(trackerCount != 0) {
					websitesStatsFile.write(name + "," + trackerCount);
					websitesStatsFile.newLine();
				}
			}
			websitesStatsFile.close();
		} catch (IOException e) {
			logMessage("Error: cannot create the stats file", 1);
			if(showDebug) e.printStackTrace();
		}
	}

	/**
	 * Prints a message in the console and writes a message in the log file.
	 * @param message the message to print and write
	 * @param type type of the message: 0 = normal; 1 = show time; 2 = add spaces; 3 = error.
	 * 		normal: just show the message
	 *		show time: add the time before the message
	 *		add spaces: add spaces to offset the lack of time before the message
	 *		error: add spaces and ">" to focus on an error
	 */
	public static void logMessage(String message, int type) {
		switch(type) {
		case 1: message = dateFormat.format(new Date()) + " - " + message; break;
		case 2: message = "                        " + message; break;
		case 3: message = "             >>>>>>>>>> " + message; break;
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
