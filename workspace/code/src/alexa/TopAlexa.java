package alexa;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import alexa.Website;

public class TopAlexa {
	private ArrayList<Website> websites;

	public TopAlexa(int begin, int end) {
		websites = new ArrayList<Website>();
		read(begin, end);
	}

	public ArrayList<Website> getWebsites() {
		return websites;
	}

	private void read(int begin, int end) {
		if(begin <= 0) {
			System.out.println("The range starts at 1");
			return;
		}
		else if(begin > 1000000 || end > 1000000) {
			System.out.println("The range ends at 1000000");
			return;
		}
		else if(begin > end) {
			System.out.println("The range is incorrect");
			return;
		}

		try {
			Scanner scanner = new Scanner(new File("top-1m.csv"));
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine();
				String[] tokens = line.split(",");
				int position = Integer.parseInt(tokens[0]);
				String url = tokens[1];

				if(position >= begin && position <= end) {
					Website website = new Website(position, url);
					websites.add(website);
				}
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
