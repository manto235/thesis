package parser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.codehaus.jackson.JsonParseException;

import edu.umass.cs.benchlab.har.*;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
//import edu.umass.cs.benchlab.har.tools.HarFileWriter;

public class Parser {

	private static boolean debug;
	private static BufferedWriter logsFile;
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
	static int countFails = 0;
	static int countSuccesses = 0;

	/**
	 * Loads the files from a directory
	 * 
	 * @param directoryName: the directory containing the files to load
	 * @return an ArrayList<File> containing all the files of the directory
	 */
	public static ArrayList<File> loadFiles(String directoryName) {
		logMessage("Info: loading the files from directory \"" + directoryName + "\"... ");
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			logMessage("Error: the directory does not exist!");
			System.exit(1);
		}

		File[] files = directory.listFiles();
		ArrayList<File> filesList = new ArrayList<File>();

		for (File file : files) {
			if(file.isFile()) {
				filesList.add(file);
			}
		}

		logMessage("      Done!");
		return filesList;
	}

	public static void launchParser(String directoryName, boolean showDebug) {
		debug = showDebug;
		String start = "----------------------------------------\n"
				+ dateFormat.format(new Date()) + " - Launching parser...\n"
				+ "   directory: " + directoryName + "\n";
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

		// Load the list of files
		ArrayList<File> filesList = loadFiles(directoryName);

		for (File file : filesList) {
			logMessage("Info: parsing " + file.getName() + "...");
			parseHARfile(file);
		}

		logMessage(" ----- Summary -----");
		logMessage("  " + filesList.size() + " files");
		logMessage("  " + countFails + " fails");
		logMessage("  " + countSuccesses + " successes");

		closeLogFile();
	}

	public static void parseHARfile(File file) {
		try {
			HarFileReader r = new HarFileReader();
			List<HarWarning> warnings = new ArrayList<HarWarning>();
			HarLog log = r.readHarFile(file, warnings);
			for (HarWarning w : warnings)
				System.out.println("File: " + file.getName() + " - Warning: " + w);

			//HarFileWriter w = new HarFileWriter();

			// Access all elements as objects
			//HarBrowser browser = log.getBrowser();
			HarEntries entries = log.getEntries();
			List<HarEntry> entriesList = entries.getEntries();

			for (HarEntry entry : entriesList) {
				System.out.println("Entry (request URL) : " + entry.getRequest().getUrl());
				//System.out.println("-- Entry (response) : " + entry.getResponse());
				//System.out.println("> Entry (response CONTENT MIMETYPE) : " + entry.getResponse().getContent().getMimeType());
			}

			// Once you are done manipulating the objects, write back to a file
			//System.out.println("Writing " + fileName + ".parsed");
			//File f2 = new File(fileName + ".parsed");
			//w.writeHarFile(log, f2);
			countSuccesses++;
		}
		catch (JsonParseException e)
		{
			e.printStackTrace();
			System.out.println("Parsing error : " + file.getName());
			countFails++;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println("IO exception : " + file.getName());
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
