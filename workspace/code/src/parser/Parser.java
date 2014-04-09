package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
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

import org.xbill.DNS.Address;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.Type;

import com.google.common.net.InetAddresses;
import com.google.common.net.InternetDomainName;

import edu.umass.cs.benchlab.har.HarEntries;
import edu.umass.cs.benchlab.har.HarEntry;
import edu.umass.cs.benchlab.har.HarLog;
import edu.umass.cs.benchlab.har.HarWarning;
import edu.umass.cs.benchlab.har.tools.HarFileReader;

public class Parser {

	private static boolean showDebug;
	private static boolean showTrackers;
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
	private static RegexGhostery regexGhostery;
	/**
	 * The latest file among files of a website
	 */
	private static Map<String, Integer> filesLatest;
	/**
	 * The occurrences found of a tracker (Ghostery)
	 */
	private static Map<String, Integer> trackersGhosteryStats;
	/**
	 * The number of trackers found on a website
	 */
	private static Map<String, Integer> websitesStats;
	/**
	 * The detailed numbers of trackers found on a website
	 */
	private static Map<String, int[]> websitesDetailedStats;
	private static int countSuccesses = 0;
	private static ArrayList<String> filesFailed = new ArrayList<String>();

	//TODO: delete
	private static Map<String, Integer> mimetype;
	private static BufferedWriter logsFil;

	public static void launchParser(String directoryName, boolean debug, boolean trackers) {
		showDebug = debug;
		showTrackers = trackers;
		String start = dateFormat.format(new Date()) + " - Launching parser...\n"
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
			logsFil = new BufferedWriter(new FileWriter(new File(directoryName+"/logs/log_fails.txt"), true));
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the logs file.\n"
					+ "> Please check your file system permissions.");
			System.exit(1);
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
		trackersGhosteryStats = new HashMap<String, Integer>();
		for(String trackerName : regexGhostery.getRegex().values()) {
			trackersGhosteryStats.put(trackerName, 0);
		}

		// Initialize the Map for the websites statistics
		websitesStats = new HashMap<String, Integer>();
		websitesDetailedStats = new HashMap<String, int[]>();
		mimetype = new HashMap<String, Integer>();

		// Load the list of files
		ArrayList<File> filesList = loadFiles(directoryName);

		// Total number of trackers for the entire analysis
		int totalTrackers = 0;

		// Parse each file
		for (File file : filesList) {
			logMessage("Parsing " + file.getName() + "...", 1);
			int websiteTrackers = parseHARfile(file);
			if(websiteTrackers != -1) {
				totalTrackers += websiteTrackers;
			}
		}

		logMessage("Info: the parsing of the files is done!", 1);
		logMessage("Total number of trackers: " + totalTrackers, 2);

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
			logMessage("----- Files failed -----", 0);
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
	 * @return an ArrayList<File> containing all the files of the directory to analyze
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
		filesLatest = new HashMap<String, Integer>();
		ArrayList<File> filesList = new ArrayList<File>();

		for (File file : files) {
			if(file.isFile()) {
				String website = file.getName();
				// Remove ".har" from the filename
				website = website.substring(0, website.indexOf(".har"));
				// Get the current version of the file
				String version = website.substring(website.lastIndexOf("-")+1, website.length());
				int currentVersion;
				try {
					currentVersion = Integer.parseInt(version);
					website = website.substring(0, website.lastIndexOf("-"));
				} catch (NumberFormatException nfe) {
					currentVersion = 0;
				}
				// Update the map with the latest version
				if(!filesLatest.containsKey(website)) {
					filesLatest.put(website, currentVersion);
				}
				else {
					int latestVersion = filesLatest.get(website);
					if(currentVersion > latestVersion) {
						filesLatest.put(website, currentVersion);
					}
				}
			}
		}

		for (String website : filesLatest.keySet()) {
			int version = filesLatest.get(website);
			if(version == 0) {
				filesList.add(new File(directoryName + "/" + website + ".har"));
			}
			else {
				filesList.add(new File(directoryName + "/"+ website + "-" + version + ".har"));
			}
		}
		return filesList;
	}

	/**
	 * Called for each website: parses its HAR file.
	 * Saves the detailed number of trackers in the Map websitesDetailedStats.
	 *
	 * @param file the HAR file
	 * @return the number of trackers found on the website
	 */
	public static int parseHARfile(File file) {
		try {
			int[] results = {0, 0, 0};
			String website = file.getName();
			// Remove ".har" from the filename
			website = website.substring(0, website.indexOf(".har"));
			String version = website.substring(website.lastIndexOf("-")+1, website.length());
			try {
				Integer.parseInt(version); // If there is no version, throw an error
				website = website.substring(0, website.lastIndexOf("-"));
			} catch (NumberFormatException nfe) {
				// Nothing to do
			}
			logMessage("Website: " + website, 2);

			HarFileReader reader = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = reader.readHarFile(file, warnings);
			for (HarWarning warning : warnings)
				logMessage("Warning: " + warning, 3);

			// Access all elements as objects
			HarEntries entries = log.getEntries();
			List<HarEntry> entriesList = entries.getEntries();

			int countTrackersGhostery = 0;
			int countJSAnotherDomain = 0;
			int countTrackingPixels = 0;
			String mainSOA = null;

			String mainHost = new URL("http://" + website).getHost();
			// If the URL is an IP, try to get the associated domain
			if(InetAddresses.isInetAddress(mainHost)) {
				String message = "Info: transformed IP " + mainHost + " to ";
				mainHost = Address.getHostName(Address.getByAddress(mainHost));
				message = message + mainHost;
				logMessage(message, 3);
			}
			InternetDomainName mainDomain = InternetDomainName.from(mainHost);
			// Get the top domain
			while(mainDomain.parent().hasParent()) {
				mainDomain = mainDomain.parent();
			}
			Name mainName = Name.fromString(mainDomain.toString());
			Lookup mainLookup = new Lookup(mainName, Type.SOA);
			Record mainRecords[] = mainLookup.run();

			if(mainRecords != null && mainRecords.length > 0 && mainRecords[0] instanceof SOARecord) {
				mainSOA = ((SOARecord)mainRecords[0]).getAdmin().toString();
			}

			// Analyze every entry
			for (HarEntry entry : entriesList) {
				if(checkRegexGhostery((entry.getRequest().getUrl()))) {
					countTrackersGhostery++;
				}
				else {
					String currentUrl = entry.getRequest().getUrl();
					String currentHost = new URL(currentUrl).getHost();
					// If the URL is an IP, try to get the associated domain
					if(InetAddresses.isInetAddress(currentHost)) {
						String message = "Info: transformed IP " + currentHost + " to ";
						currentHost = Address.getHostName(Address.getByAddress(currentHost));
						message = message + currentHost;
						logMessage(message, 3);
					}

					InternetDomainName currentDomain = InternetDomainName.from(currentHost);
					// Get the top domain
					while(currentDomain.parent().hasParent()) {
						currentDomain = currentDomain.parent();
					}
					Name currentName = Name.fromString(currentDomain.toString());
					Lookup currentLookup = new Lookup(currentName, Type.SOA);
					Record currentRecords[] = currentLookup.run();

					String currentSOA = null;
					if(currentRecords != null && currentRecords.length > 0 && currentRecords[0] instanceof SOARecord) {
						currentSOA = ((SOARecord)currentRecords[0]).getAdmin().toString();
					}
					//System.out.println("-- Entry (request) : " + entry.getRequest());
					//System.out.println("-- Entry (response) : " + entry.getResponse());
					//System.out.println("> Entry (response CONTENT MIMETYPE) : " + entry.getResponse().getContent().getMimeType());

					if(mainSOA != null && currentSOA != null) {
						if(!mainSOA.equals(currentSOA)) {
							int value = 0;
							if(mimetype.containsKey(entry.getResponse().getContent().getMimeType())){
								value = mimetype.get(entry.getResponse().getContent().getMimeType());
							}
							mimetype.put(entry.getResponse().getContent().getMimeType(), value+1);
							// Check JS
							if(entry.getResponse().getContent().getMimeType().equals("application/x-javascript")) {
								//System.out.println("Different SOA for JS: " + mainSOA + " vs " + currentSOA + " for " + currentUrl);
								countJSAnotherDomain++;
							}
							//if(image)
							//final String size = bi.getWidth() + "x" + bi.getHeight();
						}
					}
					else {
						String message = "!!!!! Cannot compare SOA: ";
						if(mainSOA == null) {
							message += "cannot get SOA of website : " + mainName;
						}
						else if(currentSOA == null) {
							message += "cannot get SOA of URL : " + currentName;
						}
						System.out.println(message);
						logsFil.write(message);
						logsFil.newLine();
					}
				}
			}

			countSuccesses++;
			logMessage("Number of trackers found (Ghostery): " + countTrackersGhostery, 2);
			logMessage("Number of JS from another domain: " + countJSAnotherDomain, 2);
			logMessage("Number of tracking pixels: " + countTrackingPixels, 2);

			results[0] = countTrackersGhostery;
			results[1] = countJSAnotherDomain;
			results[2] = countTrackingPixels;
			int totalNumberTrackers = countTrackersGhostery + countJSAnotherDomain + countTrackingPixels;
			websitesStats.put(website, totalNumberTrackers);
			websitesDetailedStats.put(website, results);
			return totalNumberTrackers;
		}
		catch (Exception e) {
			logMessage("Error: cannot parse the file.", 3);
			filesFailed.add(file.getName());
			if(showDebug) e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Called for each URL: checks if the URL is known as a tracker in the Ghostery database.
	 * If the URL is a tracker, increments the counter of this tracker.
	 *
	 * @param url the URL to check.
	 * @return true if the URL is a tracker, false otherwise.
	 */
	public static boolean checkRegexGhostery(String url) {
		Map<String, String> regex = regexGhostery.getRegex();
		for(String singleRegex : regex.keySet()) {
			Pattern pattern = Pattern.compile(singleRegex);
			Matcher matcher = pattern.matcher(url);
			if(matcher.find()) {
				// Increment the counter of this tracker
				int trackerCount = trackersGhosteryStats.get(regex.get(singleRegex));
				trackersGhosteryStats.put(regex.get(singleRegex), trackerCount+1);
				if(showTrackers) {
					logMessage("    Tracker found (Ghostery): " + url + "\n"
							+ "        " + singleRegex + " from " + regex.get(singleRegex), 0);
				}
				return true;
			}
		}
		return false;
	}

	public static void computeStats(String directoryName) {
		try {
			// TRACKERS
			BufferedWriter trackersStatsFile = new BufferedWriter(new FileWriter(new File(directoryName+"/logs/stats_trackers.csv"), false));

			//statsTrackers.write("----- Statistics of trackers -----");
			//statsTrackers.newLine();
			//statsTrackers.write("> Number of trackers entities: " + trackersStats.size());
			//statsTrackers.newLine();

			List<Map.Entry<String, Integer>> trackersEntries = new LinkedList<Map.Entry<String, Integer>>(trackersGhosteryStats.entrySet());
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
				int trackerCount = trackersGhosteryStats.get(name);
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

			// MIMETYPE
			BufferedWriter mimetypefile = new BufferedWriter(new FileWriter(new File(directoryName+"/logs/stats_mimetypes.csv"), false));

			List<Map.Entry<String, Integer>> mimEntries = new LinkedList<Map.Entry<String, Integer>>(mimetype.entrySet());
			Collections.sort(mimEntries, new Comparator<Map.Entry<String, Integer>>() {
				@Override
				public int compare(Entry<String, Integer> o1, Entry<String, Integer> o2) {
					return o2.getValue().compareTo(o1.getValue());
				}
			});

			Map<String, Integer> sortedmim = new LinkedHashMap<String, Integer>();
			for(Map.Entry<String, Integer> entry: mimEntries){
				sortedmim.put(entry.getKey(), entry.getValue());
			}

			for(String name : sortedmim.keySet()) {
				int trackerCount = mimetype.get(name);
				if(trackerCount != 0) {
					mimetypefile.write(name + "," + trackerCount);
					mimetypefile.newLine();
				}
			}
			mimetypefile.close();
		} catch (IOException e) {
			logMessage("Error: cannot create the stats file", 1);
			if(showDebug) e.printStackTrace();
		}
	}

	/**
	 * Prints a message in the console and writes a message in the log file.
	 * @param message the message to print and write
	 * @param type type of the message:<br>
	 * 		- 0 (normal): just show the message.<br>
	 *		- 1 (show time): add the time before the message.<br>
	 *		- 2 (add spaces): add spaces to offset the lack of time before the message.<br>
	 *		- 3 (focus): add spaces and ">" to focus on a message.<br>
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
	 * 	Closes the logs file.<br>
	 *  If a problem occurs, prints a message in the console.
	 */
	public static void closeLogFile() {
		try {
			logsFile.write("----------------------------------------");
			logsFile.newLine();
			logsFile.close();
			logsFil.close();
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot close the logs file.\n> It may be corrupted.");
			if(showDebug) ioe.printStackTrace();
		}
	}
}
