package crawler;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.UnhandledAlertException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.UnreachableBrowserException;

import crawler.WebsitesList;
import crawler.Website;

public class Crawler {

	private static boolean debug;
	private static WebDriver driver;
	private static FileWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
	private static ArrayList<String> websitesFailed = new ArrayList<String>();
	private static ArrayList<String> websitesPotentiallyFailed = new ArrayList<String>();
	private static int websitesVisited = 0;

	public static void launchCrawler(String directoryName, String ffprofile, String websitesFile,
			int startIndex, int endIndex, int attempts, boolean showDebug, int restart) {
		debug = showDebug;
		String start = dateFormat.format(new Date()) + " - Launching crawler...\n"
				+ "   directory: " + directoryName + ", websites file: " + websitesFile + "\n"
				+ "   start index: " + startIndex + ", end index: " + endIndex + "\n"
				+ "   Firefox profile: " + ffprofile + ", restart value: " + restart + "\n"
				+ "   number of attempts per website: " + attempts;
		System.out.println(start);

		// Check the file system permissions
		try {
			if(!checkDirectories(directoryName)) {
				System.out.println(dateFormat.format(new Date()) + " - Error: cannot create the directory containing the outputs.\n"
						+ "> Please check your file system permissions.");
				System.exit(1);
			}

			logsFile = new FileWriter(new File(directoryName+"/logs/log_crawler.txt"), true);
			logsFile.write(start);
			logsFile.write(System.getProperty("line.separator"));
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the logs file.\n"
					+ "> Please check your file system permissions.");
			System.exit(1);
		}

		// Manage the signals
		// Note: placed after the check of file permissions because this check is really fast + uses logMessage
		Runtime.getRuntime().addShutdownHook(new Thread()
		{
			@Override
			public void run()
			{
				logMessage("Terminating now...", 1);
				haltDriver();
				closeLogFile();
			}
		});

		// Get the list of websites
		logMessage("Loading the list of websites...", 1);
		WebsitesList websites = new WebsitesList(websitesFile, startIndex, endIndex);
		if(websites.getWebsites().size() == 0) {
			logMessage("Error: the list of websites is empty.", 1);
			logMessage(websites.getStatus(), 3);
			System.exit(1);
		}
		else {
			logMessage("Info: " + websites.getStatus(), 1);
		}

		// Initialize the driver
		logMessage("Initializing the driver...", 1);
		initializeDriver(directoryName, ffprofile);

		for(Website website : websites.getWebsites()) {
			// Restart Firefox
			if(websitesVisited % restart == 0 && websitesVisited != 0) {
				logMessage("Restarting Firefox...", 1);
				driver.quit();
				initializeDriver(directoryName, ffprofile);
			}

			boolean success = false;
			int attempt = 1;

			do {
				try {
					logMessage("Crawling website #" + website.getPosition() + " - " + website.getUrl()
							+ " (attempt #" + attempt + ").", 1);
					driver.get("http://" + website.getUrl());

					// Wait till HAR is exported
					try {
						System.out.println("                        Waiting 8 seconds"
								+ " for the HAR file to be exported...");
						Thread.sleep(8000);
					} catch (InterruptedException e) {
						if(debug) e.printStackTrace();
					}
					success = true;
				} catch (TimeoutException te) {
					logMessage("Error: website " + website.getUrl()
							+ " was not successfully loaded (timeout).", 3);
					attempt++;
					// Add the website to the list of potentially failed websites at the 2nd attempt
					if(attempt == 2) {
						websitesPotentiallyFailed.add(website.getUrl());
					}
					if(debug) te.printStackTrace();
					// Move to the blank page before retrying to load the website
					try {
						driver.get("about:blank");
						Thread.sleep(5000); // It's necessary to give time to the browser
					} catch (InterruptedException ie) {
						if(debug) ie.printStackTrace();
					}
				} catch (UnreachableBrowserException e) {
					logMessage("Critical error: cannot communicate with the remote browser."
							+ " Don't close Firefox!", 1);
					if(debug) e.printStackTrace();
					System.exit(1);
				} catch (UnhandledAlertException e) {
					logMessage("Error: website " + website.getUrl()
							+ " was not successfully loaded (error).", 3);
					if(debug) e.printStackTrace();
					// Skip the website and consider it as failed
					websitesFailed.add(website.getUrl());
					break;
				}
			} while(attempt <= attempts && !success);
			websitesVisited++;

			// The website failed after several attempts
			if(attempt >= attempts && !success) {
				websitesFailed.add(website.getUrl());
				websitesPotentiallyFailed.remove(website.getUrl());
			}
		}

		logMessage("Info: the crawling of the websites is done!", 1);

		deleteUselessFiles(directoryName);

		detailProblematicWebsites();

		System.exit(0);
	}

	/**
	 * Check if the directories exist and create them if needed
	 *
	 * @param directoryName the directory to check
	 * @return true if the directories exists (or have been created), false otherwise
	 */
	public static boolean checkDirectories(String directoryName) {
		boolean directoriesOK = true;

		File directory = new File(directoryName);
		File logsDirectory = new File(directoryName + "/logs/");

		// The directory does not exist: create both the directory and the logs subdirectory
		if(!directory.isDirectory()) {
			if(logsDirectory.mkdirs()) {
				System.out.println("Info: a directory named \"" + directoryName + "\" has been created.\n"
						+ "      a subdirectory named \"logs\" has also been created.");
			}
			else {
				directoriesOK = false;
			}
		}
		// The directory already exists: check if the logs subdirectory also exist
		else {
			if(!logsDirectory.isDirectory()) {
				if(logsDirectory.mkdirs()) {
					System.out.println("Info: a subdirectory named \"logs\" has been created.");
				}
				else {
					directoriesOK = false;
				}
			}
		}

		return directoriesOK;
	}

	/**
	 * Initialize the driver.
	 *
	 * @param directoryName: the directory in which the files will be written.
	 * @param ffprofile the Firefox profile to use
	 */
	public static void initializeDriver(String directoryName, String ffprofile) {
		try {
			// Configure it as a desired capability
			FirefoxProfile profile = new ProfilesIni().getProfile(ffprofile);
			profile.setAcceptUntrustedCertificates(true);
			profile.setAssumeUntrustedCertificateIssuer(true);
			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(FirefoxDriver.PROFILE, profile);

			// ----- Firebug + NetExport -----
			// Set default Firefox preferences
			profile.setPreference("app.update.enabled", false);
			String domain = "extensions.firebug.";

			// Set default Firebug preferences
			profile.setPreference(domain + "allPagesActivation", "on");
			profile.setPreference(domain + "breakOnErrors", false);
			profile.setPreference(domain + "showBreakNotification", false);
			profile.setPreference(domain + "defaultPanelName", "net");
			profile.setPreference(domain + "net.enableSites", true);
			profile.setPreference(domain + "console.enableSites", false);
			profile.setPreference(domain + "cookies.enableSites", false);
			profile.setPreference(domain + "script.enableSites", false);

			// Set default NetExport preferences
			profile.setPreference(domain + "netexport.alwaysEnableAutoExport", true);
			profile.setPreference(domain + "netexport.compress", false);
			profile.setPreference(domain + "netexport.showPreview", false);
			profile.setPreference(domain + "netexport.defaultLogDir", System.getProperty("user.dir")+"/"+directoryName);

			// Start the browser up
			driver = new FirefoxDriver(capabilities);
			driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);

			// Wait till Firebug is loaded
			Thread.sleep(5000);

			logMessage("Info: WebDriver is ready.", 1);
		}
		catch (NullPointerException e) {
			logMessage("Error: the Firefox profile " + ffprofile + " has not been found.", 1);
			if(debug) e.printStackTrace();
			System.exit(1);
		}
		catch (Exception e) {
			logMessage("Error: cannot initialize the driver.", 1);
			if(debug) e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Deletes the useless files.
	 * These are the files generated when visiting the "about:blank" page when retrying another attempt.
	 * @param directoryName the name of the directory containing the files
	 */
	public static void deleteUselessFiles(String directoryName) {
		for (File file : new File(directoryName).listFiles()) {
			if(file.isFile()) {
				String filename = file.getName();
				if(filename.equals(".har") || filename.substring(1, filename.length()-4).matches("\\d+")) {
					file.delete();
				}
			}
		}
	}

	/**
	 * Details the websites for which problems occurred.
	 */
	public static void detailProblematicWebsites() {
		if(!websitesPotentiallyFailed.isEmpty()) {
			logMessage("", 0);
			logMessage("The following websites potentially failed (more than one attempt):", 0);
			for(String websitePotentiallyFailed : websitesPotentiallyFailed) {
				logMessage(websitePotentiallyFailed, 0);
			}
		}

		if(!websitesFailed.isEmpty()) {
			logMessage("", 0);
			logMessage("The following websites failed:", 0);
			for(String websiteFailed : websitesFailed) {
				logMessage(websiteFailed, 0);
			}
		}
	}

	/**
	 * Quits the driver.
	 */
	public static void haltDriver() {
		if(driver != null) {
			try {
				driver.quit();
				logMessage("Info: the driver has been halted successfully.", 1);
			} catch (UnreachableBrowserException e) {
				// Do nothing
			} catch (Exception e) {
				logMessage("Error: the driver was not halted successfully.", 1);
				if(debug) e.printStackTrace();
			}
		}
	}

	/**
	 * Prints a message in the console and writes a message in the log file.
	 * @param message the message to print and write.
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
			logsFile.write(System.getProperty("line.separator"));
			logsFile.flush();
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
			logsFile.write(System.getProperty("line.separator"));
			logsFile.close();
			System.out.println(dateFormat.format(new Date()) + " - Info: log file successfully closed.");
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot close the logs file.\n> It may be corrupted.");
			if(debug) ioe.printStackTrace();
		}
	}
}
