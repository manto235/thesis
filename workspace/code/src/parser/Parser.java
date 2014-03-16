package parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.JsonParseException;

import edu.umass.cs.benchlab.har.*;
import edu.umass.cs.benchlab.har.tools.HarFileReader;
//import edu.umass.cs.benchlab.har.tools.HarFileWriter;

public class Parser {

	static int count = 0;
	/**
	 * Loads the files from a directory
	 * 
	 * @param directoryName: the directory containing the files to load
	 * @return an ArrayList<File> containing all the files of the directory
	 */
	public static ArrayList<File> loadFiles(String directoryName) {
		System.out.print("Loading the files from directory \"" + directoryName + "\"... ");
		File directory = new File(directoryName);
		if(!directory.isDirectory()) {
			System.out.println("The directory does not exist!");
			System.exit(1);
		}

		File[] files = directory.listFiles();
		ArrayList<File> filesList = new ArrayList<File>();

		for (File file : files) {
			if(file.isFile()) {
				filesList.add(file);
			}
		}

		System.out.println("Done!");
		return filesList;
	}

	public static void launchParser(String directoryName) {
		ArrayList<File> filesList = loadFiles(directoryName);

		for (File file : filesList) {
			System.out.println("Parsing " + file.getName() + "...");
			parseHARfile(file);
		}
		System.out.println("TOTAL FAIL : " + count);
	}

	public static void parseHARfile(File file) {
		try {
			HarFileReader r = new HarFileReader();
			//HarLog log = r.readHarFile(file);

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
				//System.out.println("Entry (request URL) : " + entry.getRequest().getUrl());
				//System.out.println("-- Entry (response) : " + entry.getResponse());
				//System.out.println("> Entry (response CONTENT MIMETYPE) : " + entry.getResponse().getContent().getMimeType());
			}

			/*List<HarPage> pages = log.getPages().getPages();
			for (HarPage page : pages)
			{
				System.out.println("page start time: " + ISO8601DateFormatter.format(page.getStartedDateTime()));
				System.out.println("page id: " + page.getId());
				System.out.println("page title: "+page.getTitle());
			}*/

			// Once you are done manipulating the objects, write back to a file
			//System.out.println("Writing " + fileName + ".parsed");
			//File f2 = new File(fileName + ".parsed");
			//w.writeHarFile(log, f2);
		}
		catch (JsonParseException e)
		{
			e.printStackTrace();
			System.out.println("Parsing error (HAR) : " + file.getName());
			count++;
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.out.println("IO exception (HAR) : " + file.getName());
		}
	}

	public static void parseIMGfile(File file) {
		// UTILISE PAR LE PARSER D'IMAGES
		/*try {
			URL url = new URL(src);
			System.out.println(url.getHost());
			//if(url.getHost())
			System.out.println(src);
		} catch (MalformedURLException e) {
			System.out.println("Error while creating the URL");
		}*/
	}

}
