package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import net.lightbody.bmp.core.har.Har;
import net.lightbody.bmp.proxy.ProxyServer;

import org.openqa.selenium.By;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;
import org.openqa.selenium.firefox.internal.ProfilesIni;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;

import edu.umass.cs.benchlab.har.tools.HarFileReader;
import alexa.TopAlexa;
import alexa.Website;

public class Crawler {

	private static ProxyServer proxy;
	private static WebDriver driver;
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

	/**
	 * Initialize the proxy and the driver.
	 * 
	 * @param port: the port used by the proxy.
	 */
	public static void initializeProxyandDriver(int port) {
		try {
			proxy = new ProxyServer(port);
			// Start the proxy
			proxy.start();
			proxy.setCaptureHeaders(true);
			proxy.setCaptureContent(true);
			// Get the Selenium proxy object
			Proxy seleniumProxy = proxy.seleniumProxy();

			// Configure it as a desired capability
			FirefoxProfile profile = new ProfilesIni().getProfile("Selenium");
			profile.setAcceptUntrustedCertificates(true);
			profile.setAssumeUntrustedCertificateIssuer(true);
			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(FirefoxDriver.PROFILE, profile);
			capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

			// Start the browser up
			driver = new FirefoxDriver(capabilities);

			System.out.println(dateFormat.format(new Date()) + " - Info: ProxyServer and WebDriver are ready.");
			logsFile.write(dateFormat.format(new Date()) + " - Info: ProxyServer and WebDriver are ready.");
			logsFile.newLine();
		}
		catch (Exception e) {
			System.out.println(dateFormat.format(new Date()) +  " - Error: cannot initialize the proxy and the driver.");
			try {
				logsFile.write(dateFormat.format(new Date()) +  " - Error: cannot initialize the proxy and the driver.");
				logsFile.newLine();
			} catch (IOException ioe) {}
		}
	}

	public static void launchCrawler(String directoryName, int port, String file, int beginIndex, int endIndex, int attempts) {
		String start = "----------------------------------------\n"
				+ dateFormat.format(new Date()) + " - Launching crawler...\n"
				+ "   directory: " + directoryName + "\n"
				+ "   port: " + port + ", file : " + file + "\n"
				+ "   begin index: " + beginIndex + ", end index: " + endIndex + "\n"
				+ "   number of attempts per website: " + attempts;
		System.out.println(start);
		try {
			logsFile = new BufferedWriter(new FileWriter(new File("logs.txt"), true));
			logsFile.write(start);
			logsFile.newLine();
		} catch (IOException e) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the logs files.\n> Please check your file system permissions.");
			return;
		}

		// Check if the directory exists and creates it if needed
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			if(directory.mkdirs()) {
				System.out.println(dateFormat.format(new Date()) + " - Info: a directory named \"" + directoryName + "\" has been created.");
				try{
					logsFile.write(dateFormat.format(new Date()) + " - Info: a directory named \"" + directoryName + "\" has been created.");
					logsFile.newLine();
				} catch (IOException ioe) {}
			}
			else {
				System.out.println(dateFormat.format(new Date()) + " - Error: cannot create the directory containing the outputs.\n"
						+ "> Please, create a directory named \"" + directoryName + "\".");
				try {
					logsFile.write(dateFormat.format(new Date()) + " - Error: cannot create the directory containing the outputs.\n"
							+ "> Please, create a directory named \"" + directoryName + "\".");
					logsFile.newLine();
				}  catch (IOException ioe) {}
				haltProxyAndDriver();
				return;
			}
		}

		// Get the list of websites and initialize the proxy & the driver
		TopAlexa websites = new TopAlexa(file, beginIndex, endIndex);
		initializeProxyandDriver(port);

		for(Website website : websites.getWebsites()) {
			int attempt = 1;
			boolean fileOK;
			do {
				writeFiles(website, directoryName, attempt);
				// We check if the file is OK
				fileOK = checkHARfile(website, directoryName, attempt);
				if(attempt != 1) {
					try {
						Thread.sleep(3000);
						System.out.println("Waiting for 3 seconds");
					} catch (InterruptedException e) {}
				}
				attempt++;
			}
			while(!fileOK && attempt <= attempts);
		}

		// Close the logs files
		try {
			logsFile.close();
		} catch (IOException e) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot close the logs files.\n> They may be corrupted.");
		}
		haltProxyAndDriver();
	}

	public static void writeFiles(Website website, String directoryName, int attempt) {
		System.out.println(dateFormat.format(new Date()) + " - Crawling website #" + website.getPosition() + " - " + website.getUrl() + " (attempt #" + attempt + ").");
		try {
			logsFile.write(dateFormat.format(new Date()) + " - Crawling website #" + website.getPosition() + " - " + website.getUrl() + " (attempt #" + attempt + ").");
			logsFile.newLine();
		} catch (IOException ioe) {}

		// Create a new HAR with the appropriate label
		proxy.newHar(website.getUrl());
		// Open the website
		driver.get("http://" + website.getUrl());
		// Get the HAR data
		Har har = proxy.getHar();

		String filename = directoryName + "/" + website.getPosition() + "-" + website.getUrl() + attempt;

		// Write the HAR file
		File output = new File(filename + "_HAR");
		try {
			har.writeTo(output);
		} catch (Exception e) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the file: " + filename + "_HAR.");
			try {
				logsFile.write(dateFormat.format(new Date()) + " - Error: cannot write the file: " + filename + "_HAR.");
				logsFile.newLine();
			} catch (IOException ioe) {}
		}

		// Write the IMG file
		List<WebElement> allImages = driver.findElements(By.tagName("img"));
		try {
			BufferedWriter images = new BufferedWriter(new FileWriter(new File(filename + "_IMG"), false));
			for (WebElement image: allImages) {
				images.write(image.getAttribute("src") + "," + image.getAttribute("height") + "," + image.getAttribute("width") + "," + image.isDisplayed() + "," + image.isEnabled());
				images.newLine();
			}
			images.close();
		} catch (Exception e) {
			System.out.println(dateFormat.format(new Date()) + " - Error: cannot write the file: " + filename + "_IMG.");
			try {
				logsFile.write(dateFormat.format(new Date()) + " - Error: cannot write the file: " + filename + "_IMG.");
				logsFile.newLine();
			} catch (IOException ioe) {}
		}
	}

	public static boolean checkHARfile(Website website, String directoryName, int attempt) {
		boolean status = true;
		String filename = directoryName + "/" + website.getPosition() + "-" + website.getUrl() + attempt;
		File file = new File(filename + "_HAR");
		HarFileReader r = new HarFileReader();
		try {
			r.readHarFile(file);
		} catch (Exception e) {
			System.out.println(dateFormat.format(new Date()) + " - Error: file " + filename + " is wrong.");
			try {
				status = false;
				logsFile.write(dateFormat.format(new Date()) + " - Error: file " + filename + " is wrong.");
				logsFile.newLine();
			} catch (IOException ioe) {}
		}
		return status;
	}

	/**
	 * Stops the proxy and quits the driver.
	 */
	public static void haltProxyAndDriver() {
		try {
			proxy.stop();
		} catch (Exception e) {
			System.out.println(dateFormat.format(new Date()) + " - Error: the proxy was not stopped successfully.");
			try {
				logsFile.write(dateFormat.format(new Date()) + " - Error: the proxy was not stopped successfully.");
				logsFile.newLine();
			} catch (IOException ioe) {}
		}
		driver.quit();
		System.out.println(dateFormat.format(new Date()) + " - Info: the proxy has been stopped successfully.");
		try {
			logsFile.write(dateFormat.format(new Date()) + " - Info: the proxy has been stopped successfully.");
			logsFile.newLine();
		} catch (IOException ioe) {}
	}
}
