package crawler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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

import alexa.TopAlexa;
import alexa.Website;

public class Crawler {

	private static ProxyServer proxy;
	private static WebDriver driver;

	/**
	 * Initialize the proxy and the driver.
	 * 
	 * @param port: the port used by the proxy.
	 */
	public static void initializeProxyandDriver(int port) {
		try {
			proxy = new ProxyServer(port);
			// start the proxy
			proxy.start();
			//server.setCaptureHeaders(true);
			// get the Selenium proxy object
			Proxy seleniumProxy = proxy.seleniumProxy();

			// configure it as a desired capability
			FirefoxProfile profile = new ProfilesIni().getProfile("Selenium");
			//profile.setAcceptUntrustedCertificates(true);
			//profile.setAssumeUntrustedCertificateIssuer(true);
			DesiredCapabilities capabilities = new DesiredCapabilities();
			capabilities.setCapability(FirefoxDriver.PROFILE, profile);
			capabilities.setCapability(CapabilityType.PROXY, seleniumProxy);

			// Start the browser up
			driver = new FirefoxDriver(capabilities);

			System.out.println("ProxyServer and WebDriver are ready!");
		}
		catch (Exception e) {
			System.out.println("An error occurred while initializing the proxy and the driver.");
			e.printStackTrace();
		}
	}

	public static void launchCrawler(String directoryName, int port, int begin_index, int end_index) {
		// Check if the directory exists and creates it if needed
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			if(directory.mkdirs()) {
				System.out.println("Info: a directory named \"" + directoryName + "\" has been created");
			}
			else {
				System.out.println("An error occurred while creating the directory containing the outputs\n"
						+ "> Please, create a directory named \"" + directoryName + "\"");
				haltProxyAndDriver();
				return;
			}
		}

		TopAlexa websites = new TopAlexa(begin_index, end_index);

		initializeProxyandDriver(port);

		for(Website website : websites.getWebsites()) {
			System.out.println("Crawling website #" + website.getPosition() + " - " + website.getUrl());

			// create a new HAR with the appropriate label
			proxy.newHar(website.getUrl());

			// open the website
			driver.get("http://" + website.getUrl());

			// get the HAR data
			Har har = proxy.getHar();

			String filename = "output/" + website.getPosition() + "-" + website.getUrl();

			File output = new File(filename + "_HAR");
			try {
				har.writeTo(output);
			} catch (Exception e) {
				System.out.println("An error occurred while writing the file: " + filename + "_HAR");
			}

			// We get all the images
			List<WebElement> allImages = driver.findElements(By.tagName("img"));
			try {
				BufferedWriter images = new BufferedWriter(new FileWriter(new File(filename + "_IMG"), false));
				for (WebElement image: allImages) {
					//System.out.println(image.getAttribute("src") + "," + image.getAttribute("height") + "," + image.getAttribute("width") + "," + image.isDisplayed() + "," + image.isEnabled());
					images.write(image.getAttribute("src") + "," + image.getAttribute("height") + "," + image.getAttribute("width") + "," + image.isDisplayed() + "," + image.isEnabled());
					images.newLine();
				}
				images.close();
			} catch (Exception e) {
				System.out.println("An error occurred while writing the file: " + filename + "_IMG");
			}
		}

		haltProxyAndDriver();
	}

	/**
	 * Stops the proxy and quits the driver.
	 */
	public static void haltProxyAndDriver() {
		try {
			proxy.stop();
		} catch (Exception e) {
			System.out.println("The proxy was not stopped successfully.");
		}
		driver.quit();
		System.out.println("The proxy has been stopped successfully");
	}
}
