package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

	private static boolean debug;
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
	/**
	 * The cache of the URLs' SOA
	 */
	private static Map<String, String> cacheSOA;
	private static long startTime;
	private static int filesAnalyzed = 0;
	private static int totalFiles;
	private static int countSuccesses = 0;
	private static ArrayList<String> filesFailed = new ArrayList<String>();

	//TODO: delete
	private static Map<String, Integer> mimetype;
	private static BufferedWriter SOAFAILS;

	public static void launchParser(String directoryName, boolean showDebug, boolean trackers) {
		debug = showDebug;
		showTrackers = trackers;
		startTime = System.nanoTime();

		// Show the status every 5 minutes
		Runnable statusRunnable = new Runnable() {
			public void run() {
				double percentageAccomplished = 100 * ((double) filesAnalyzed / totalFiles);
				long elapsedTime = System.nanoTime() - startTime;
				long elapsedTimeSeconds = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
				long elapsedTimeMinutes = TimeUnit.MINUTES.convert(elapsedTime, TimeUnit.NANOSECONDS);
				long elapsedTimeHours = TimeUnit.HOURS.convert(elapsedTime, TimeUnit.NANOSECONDS);
				String time;
				if(elapsedTimeHours > 0) {
					time = "Elapsed time: " + elapsedTimeHours + " hr and " + elapsedTimeMinutes%60 + " min. ";
				}
				else if(elapsedTimeMinutes > 0) {
					time = "Elapsed time: " + elapsedTimeMinutes + " min. " ;
				}
				else {
					time = "Elapsed time: " + elapsedTimeSeconds + " sec. ";
				}
				System.out.println(time
						+ filesAnalyzed + " files analyzed out of " + totalFiles + " files"
						+ " (" + new DecimalFormat("#.#").format(percentageAccomplished) + "%).");
			}
		};
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		// Note: initialDelay not equal to zero because the number of files is not already calculated.
		executor.scheduleAtFixedRate(statusRunnable, 5, 5, TimeUnit.MINUTES);

		// Show start message
		String start = dateFormat.format(new Date()) + " - Launching parser...\n"
				+ "   directory: " + directoryName + "\n"
				+ "   debug: " + debug;
		System.out.println(start);

		// Create the logs file
		try {
			File logParser = new File(directoryName+"/logs/log_parser.txt");
			File logsDirectory = logParser.getParentFile();
			if(!logsDirectory.isDirectory() && !logsDirectory.mkdirs()) {
				System.out.println(dateFormat.format(new Date()) + " - Error: cannot create the logs folder.\n"
						+ "> Please check your file system permissions.");
				System.exit(1);
			}
			else {
				System.out.println(dateFormat.format(new Date()) + " - Info: a subdirectory named \"logs\" has been created.");
			}
			logsFile = new BufferedWriter(new FileWriter(logParser, true));
			logsFile.write(start);
			logsFile.newLine();
			SOAFAILS = new BufferedWriter(new FileWriter(new File(directoryName+"/logs/soa_fails.txt"), false));
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

		// Initialize the Map of the SOA cache
		cacheSOA = new HashMap<String, String>();

		// Load the list of files
		final ArrayList<File> filesList = loadFiles(directoryName);
		totalFiles = filesList.size();

		// Total number of trackers for the entire analysis
		int totalTrackers = 0;

		// Parse each file
		for (File file : filesList) {
			logMessage("Parsing " + file.getName() + "...", 1);
			int websiteTrackers = parseHARfile(file);
			if(websiteTrackers != -1) {
				totalTrackers += websiteTrackers;
			}
			filesAnalyzed++;
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

		long elapsedTime = System.nanoTime() - startTime;
		long elapsedTimeSeconds = TimeUnit.SECONDS.convert(elapsedTime, TimeUnit.NANOSECONDS);
		long elapsedTimeMinutes = TimeUnit.MINUTES.convert(elapsedTime, TimeUnit.NANOSECONDS);
		long elapsedTimeHours = TimeUnit.HOURS.convert(elapsedTime, TimeUnit.NANOSECONDS);
		String time;
		if(elapsedTimeHours > 0) {
			time = elapsedTimeHours + " hr and " + elapsedTimeMinutes%60 + " min.";
		}
		else if(elapsedTimeMinutes > 0) {
			time = elapsedTimeMinutes + " min." ;
		}
		else {
			time = elapsedTimeSeconds + " sec.";
		}
		logMessage("Total time: " + time, 0);
		executor.shutdown();
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
		Arrays.sort(files);
		filesLatest = new HashMap<String, Integer>();
		ArrayList<File> filesList = new ArrayList<File>();

		for (File file : files) {
			if(file.isFile()) {
				String website = file.getName();
				// Remove ".har" from the filename
				website = website.substring(0, website.length()-4);
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
			int countTrackersGhostery = 0;
			int countJSAnotherDomain = 0;
			int countTrackingPixels = 0;

			/* ----- NAME OF THE WEBSITE ----- */
			String website = file.getName();
			// Remove ".har" from the filename
			website = website.substring(0, website.length()-4);
			// Remove the version
			String version = website.substring(website.lastIndexOf("-")+1, website.length());
			try {
				Integer.parseInt(version); // If there is no version, throw an error
				website = website.substring(0, website.lastIndexOf("-"));
			} catch (NumberFormatException nfe) {
				// Nothing to do
			}
			logMessage("Website: " + website, 2);

			/* ----- READER ----- */
			HarFileReader reader = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = reader.readHarFile(file, warnings);
			for (HarWarning warning : warnings) {
				logMessage("Warning: " + warning, 3);
			}
			// Access all elements as objects
			HarEntries entries = log.getEntries();
			List<HarEntry> entriesList = entries.getEntries();

			/* ----- SOA OF THE WEBSITE ----- */
			String mainHost = new URL("http://" + website).getHost();

			String mainSOA = cacheSOA.get(mainHost);
			// Not in the cache
			if(mainSOA == null) {
				// If the URL is an IP, try to get the associated domain
				if(InetAddresses.isInetAddress(mainHost)) {
					try {
						String message = "Info: transformed IP " + mainHost + " to ";
						mainHost = Address.getHostName(Address.getByAddress(mainHost));
						message = message + mainHost;
						if(debug) System.out.println(message);
					} catch (UnknownHostException uhe) {
						// Skip this website: cannot get its hostname
						logMessage("Error: cannot get the website's hostname. Skip the website.", 3);
						return -1;
					}
				}
				try {
					InternetDomainName mainDomain = InternetDomainName.from(mainHost);

					Record mainRecords[];
					do {
						Name mainName = Name.fromString(mainDomain.toString());
						Lookup mainLookup = new Lookup(mainName, Type.SOA);
						mainRecords = mainLookup.run();
						// Try to get the SOA via the parent
						if(mainRecords == null) {
							String message = " ----- mainDomain: " + mainDomain;
							mainDomain = mainDomain.parent();
							if(debug) System.out.println(message + "  => " + mainDomain);

							// SOA of the parent found in the cache
							if(cacheSOA.containsKey(mainDomain.toString())) {
								mainSOA = cacheSOA.get(mainDomain.toString());
								// Fill up the cache (put the original host)
								cacheSOA.put(new URL("http://" + website).getHost(), mainSOA);
							}
						}
						else if(mainRecords != null && mainRecords.length > 0 && mainRecords[0] instanceof SOARecord) {
							mainSOA = ((SOARecord)mainRecords[0]).getAdmin().toString();
							// Fill up the cache with the current domain (which is a parent of the original host)
							cacheSOA.put(mainDomain.toString(), mainSOA);
							// Fill up the cache (don't put mainHost because it is modified if it's an IP => put the original host)
							cacheSOA.put(new URL("http://" + website).getHost(), mainSOA);
						}
						// Skip this website: cannot get its SOA
						else {
							logMessage("Error: cannot get the website's SOA. Skip the website.", 3);
							return -1;
						}
					}
					while(mainSOA == null && mainDomain.hasParent());
				} catch (Exception e) {
					if(debug) e.printStackTrace();
					// Peut-être passer au site suivant et supprimer le if après avec les soa null
					logMessage("Error: cannot get SOA of website: " + mainHost, 3);
					SOAFAILS.write("Error: cannot get SOA of website: " + mainHost);
					SOAFAILS.newLine();
					return -1;
				}
			}

			/* ----- ANALYZE EVERY ENTRY ----- */
			if(debug) System.out.println(" > " + entriesList.size() + " entries to analyze.");
			for (HarEntry entry : entriesList) {
				String currentUrl = entry.getRequest().getUrl();
				// Check if the URL is a tracker with the Ghostery database
				if(checkRegexGhostery(currentUrl)) {
					countTrackersGhostery++;
				}
				// Try to determine if the URL is a tracker via other means
				else {
					/* ----- SOA OF THE URL ----- */
					String currentHost = new URL(currentUrl).getHost();
					String currentSOA = cacheSOA.get(currentHost);
					// Not in the cache
					if(currentSOA == null) {
						// If the URL is an IP, try to get the associated domain
						if(InetAddresses.isInetAddress(currentHost)) {
							try {
								String message = "Info: transformed IP " + currentHost + " to ";
								currentHost = Address.getHostName(Address.getByAddress(currentHost));
								message = message + currentHost;
								if(debug) System.out.println(message);
							} catch (UnknownHostException uhe) {
								// Skip this URL: cannot get its hostname
								logMessage("Error: cannot get the URL's (" + currentHost + ") hostname. Skip the URL.", 3);
								continue;
							}
						}
						try {
							InternetDomainName currentDomain = InternetDomainName.from(currentHost);

							Record currentRecords[];
							do {
								Name currentName = Name.fromString(currentDomain.toString());
								Lookup currentLookup = new Lookup(currentName, Type.SOA);
								currentRecords = currentLookup.run();
								// Try to get the SOA via the parent
								if(currentRecords == null) {
									String message = (" ----- currentDomain: " + currentDomain);
									currentDomain = currentDomain.parent();
									if(debug) System.out.println(message + "  => " + currentDomain);

									// SOA of the parent found in the cache
									if(cacheSOA.containsKey(currentDomain.toString())) {
										currentSOA = cacheSOA.get(currentDomain.toString());
										// Fill up the cache (put the original host)
										cacheSOA.put(new URL(currentUrl).getHost(), currentSOA);
									}
								}
								else if(currentRecords != null && currentRecords.length > 0 && currentRecords[0] instanceof SOARecord) {
									currentSOA = ((SOARecord)currentRecords[0]).getAdmin().toString();
									/*if(!currentSOA.equals(currentSOAcache) && currentSOAcache != null) {
										System.out.println("><><><><>< " + new URL(currentUrl).getHost() + " : " + currentSOA + " vs " + currentSOAcache);
									}*/
									// Fill up the cache with the current domain (which is a parent of the original host)
									cacheSOA.put(currentDomain.toString(), currentSOA);
									// Fill up the cache (don't put currentHost because it is modified if it's an IP => put the original host)
									cacheSOA.put(new URL(currentUrl).getHost(), currentSOA);
								}
								// Skip this URL: cannot get its SOA
								else {
									logMessage("Error: cannot get the URL's (" + currentHost + ") SOA. Skip the URL.", 3);
									continue;
								}
							}
							while(currentSOA == null && currentDomain.hasParent());
						} catch (Exception e) {
							if(debug) e.printStackTrace();
							// Peut-être passer à l'url suivante et supprimer le if suivant avec les soa null
							logMessage("Error: cannot get SOA of URL: " + currentHost, 3);
							SOAFAILS.write("Error: cannot get SOA of URL: " + currentHost);
							SOAFAILS.newLine();
							continue;
						}
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
						SOAFAILS.write("!!! Error: an unexpected error occurred. Cannot compare the SOA.");
						SOAFAILS.newLine();
						SOAFAILS.write("website: " + mainHost + " URL: " + currentHost);
						SOAFAILS.newLine();
						/*String message;
						if(mainSOA == null) {
							logMessage("Error: cannot get SOA of website: " + mainHost, 3);
							message = "Error: cannot get SOA of website: " + mainHost;
						}
						else if(currentSOA == null) {
							logMessage("Error: cannot get SOA of URL: " + currentHost, 3);
							message = "Error: cannot get SOA of URL: " + currentHost;
						}
						else {
							logMessage("Error: an unexpected error occurred. Cannot compare the SOA.", 3);
							message = "Error: an unexpected error occurred. Cannot compare the SOA.";
						}
						SOAFAILS.write(message);
						SOAFAILS.newLine();*/
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
			if(debug) e.printStackTrace();
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
			if(debug) e.printStackTrace();
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
			if(debug) ioe.printStackTrace();
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
			SOAFAILS.close();
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot close the logs file.\n> It may be corrupted.");
			if(debug) ioe.printStackTrace();
		}
	}
}
