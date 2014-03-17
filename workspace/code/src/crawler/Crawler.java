package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.DesiredCapabilities;

import alexa.TopAlexa;
import alexa.Website;

public class Crawler {

	private static boolean debug;
	private static WebDriver driver;
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

	/**
	 * Initialize the driver.
	 * 
	 * @param directoryName: the directory in which the files will be written.
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

			// Set default NetExport preferences
			profile.setPreference(domain + "netexport.alwaysEnableAutoExport", true);
			profile.setPreference(domain + "netexport.showPreview", false);
			profile.setPreference(domain + "netexport.defaultLogDir", System.getProperty("user.dir")+"/"+directoryName);

			// Start the browser up
			driver = new FirefoxDriver(capabilities);
			driver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);

			// Wait till Firebug is loaded
			Thread.sleep(5000);

			logMessage("Info: WebDriver is ready.");
		}
		catch (Exception e) {
			logMessage("Error: cannot initialize the driver.");
			if(debug) e.printStackTrace();
			haltDriver();
			closeLogFile();
			System.exit(1);
		}
	}

	public static void launchCrawler(String directoryName, String ffprofile, String file, int beginIndex, int endIndex, int attempts, boolean showDebug) {
		debug = showDebug;
		String start = "----------------------------------------\n"
				+ dateFormat.format(new Date()) + " - Launching crawler...\n"
				+ "   directory: " + directoryName + ", file: " + file + "\n"
				+ "   begin index: " + beginIndex + ", end index: " + endIndex + "\n"
				+ "   Firefox profile: " + ffprofile + "\n"
				+ "   number of attempts per website: " + attempts;
		System.out.println(start);
		try {
			logsFile = new BufferedWriter(new FileWriter(new File("logs.txt"), true));
			logsFile.write(start);
			logsFile.newLine();
		} catch (IOException ioe) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the logs file.\n> Please check your file system permissions.");
			System.out.println("The crawler will however continue...");
			if(debug) ioe.printStackTrace();
		}

		// Check if the directory exists and creates it if needed
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			if(directory.mkdirs()) {
				logMessage("Info: a directory named \"" + directoryName + "\" has been created.");
			}
			else {
				logMessage("Error: cannot create the directory containing the outputs.\n"
						+ "> Please, create a directory named \"" + directoryName + "\".");
				haltDriver();
				closeLogFile();
				System.exit(1);
			}
		}

		// Get the list of websites and initialize the driver
		TopAlexa websites = new TopAlexa(file, beginIndex, endIndex);
		initializeDriver(directoryName, ffprofile);

		for(Website website : websites.getWebsites()) {
			boolean success;
			int attempt = 1;

			do {
				try {
					logMessage("Crawling website #" + website.getPosition() + " - " + website.getUrl() + " (attempt #" + attempt + ").");
					driver.get("http://" + website.getUrl());
					Thread.sleep(5000);
					success = true;

					// Wait till HAR is exported
					try {
						System.out.println("Waiting 3 seconds for the HAR file to be exported...");
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						if(debug) e.printStackTrace();
					}
				} catch (Exception e) {
					logMessage("Error: website " + website.getUrl() + " was not successfully loaded.");
					attempt++;
					success = false;
					if(debug) {
						if(e instanceof TimeoutException) System.out.println("TIMEOUT");
						else e.printStackTrace();
					}
				}
			} while(attempt <= attempts && !success);
		}

		haltDriver();
		closeLogFile();
	}

	/**
	 * Quits the driver.
	 */
	public static void haltDriver() {
		try {
			driver.quit();
			logMessage("Info: the driver has been halted successfully.");
		} catch (Exception e) {
			logMessage("Error: the driver was not halted successfully.");
			if(debug) e.printStackTrace();
		}
	}

	/**
	 * Prints a message in the console and writes a message in the log file.
	 * @param message the message to print and write
	 */
	public static void logMessage(String message) {
		System.out.println(dateFormat.format(new Date()) + " - " + message);
		try {
			logsFile.write(dateFormat.format(new Date()) + " - " + message);
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
