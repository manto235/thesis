package crawler;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import crawler.Website;

/**
 * Object representing a list of websites.
 *
 */
public class TopAlexa {
	private ArrayList<Website> websites;

	/**
	 * Constructor.
	 *
	 * @param file: the name of the Alexa Top file.
	 * @param begin: the beginning index of the range.
	 * @param end: the ending index of the range.
	 */
	public TopAlexa(String file, int begin, int end) {
		websites = new ArrayList<Website>();
		read(file, begin, end);
	}

	/**
	 * Gets the list of websites.
	 *
	 * @return an ArrayList of Website objects.
	 */
	public ArrayList<Website> getWebsites() {
		return websites;
	}

	/**
	 * Reads the parses the Alexa Top file.
	 *
	 * @param file: the name of the Alexa Top file.
	 * @param begin: the beginning index of the range.
	 * @param end: the ending index of the range.
	 */
	private void read(String file, int begin, int end) {
		if(begin <= 0) {
			System.out.println("The range starts at 1");
		}
		else if(begin > 1000000 || end > 1000000) {
			System.out.println("The range ends at 1000000");
		}
		else if(begin > end) {
			System.out.println("The range is incorrect");
		}
		else {
			try {
				Scanner scanner = new Scanner(new File(file));
				while(scanner.hasNextLine()) {
					String line = scanner.nextLine();
					String[] tokens = line.split(",");
					int position = Integer.parseInt(tokens[0]);
					String url = tokens[1];

					// Position is in the good range: add the website
					if(position >= begin && position <= end) {
						Website website = new Website(position, url);
						websites.add(website);
					}
					// Position is after the range: break the loop
					else if(position > end) {
						break;
					}
				}
				scanner.close();
			} catch (Exception e) {
				System.out.println("An error occurred while accessing the Alexa Top file.\n"
						+ "> Please, download it at http://www.alexa.com/topsites");
			}
		}
	}
}
