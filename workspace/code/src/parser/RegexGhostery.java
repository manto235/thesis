package parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


public class RegexGhostery {
	private Map<String, String> regex = new HashMap<String, String>();

	public RegexGhostery() {
		read();
	}

	public Map<String, String> getRegex() {
		return regex;
	}

	public void read() {
		try {
			Scanner scanner = new Scanner(new File("bugs.json"));
			scanner.useDelimiter("\\{");

			//BufferedWriter patterns = new BufferedWriter(new FileWriter(new File("Patterns.txt"), false));

			scanner.next(); // Skip the copyright note
			int count = 0;
			String content = "";
			String[] tokens;
			String name;
			String line;

			while(scanner.hasNext()) {
				count++;
				content = scanner.next();
				tokens = content.split("\"");

				name = tokens[17];
				line = tokens[11];

				line = line.replace("\\\\", ""); // Remove "\\"
				//System.out.println(line + "=" + name);
				regex.put(line, name);

				/*
				// It's a simple URL
				if(line.matches("^[a-zA-Z0-9_./-]*$")) {
					mapUrls.put(line, name);
					patterns.write(line + "=" + name);
					patterns.newLine();
				}

				else if(line.contains("[")) {
					//System.out.println(line);
				}

				// The line is surrounded by parenthesis
				else if((line.charAt(0) == '(') && line.charAt(line.length()-1) == ')') {
					line = line.substring(1, line.length()-1); // Remove the parenthesis

					// The line contains multiple URLs
					if(line.contains("|")) {
						int orSymbol;
						while((orSymbol = line.indexOf("|")) != -1) {
							// Get the URL at the beginning of the line
							mapUrls.put(line.substring(0, orSymbol), name);
							patterns.write(line.substring(0, orSymbol) + "=" + name);
							patterns.newLine();
							// Remove the URL at the beginning from the line
							line = line.substring(orSymbol+1, line.length());
						}
						// Get the last URL
						mapUrls.put(line, name);
						patterns.write(line + "=" + name);
						patterns.newLine();
					}

					// The line contains a single URL
					else {
						mapUrls.put(line, name);
						patterns.write(line + "=" + name);
						patterns.newLine();
					}
				}
				else if(line.contains("(") && line.contains(")")) {
					System.out.println(line);
				}
				else {
					System.out.println("A PARSER : "+line);
					//mapUrls.put(line, name);
					//patterns.write(line);// + "=" + name);
					//patterns.newLine();
				}*/

			}

			scanner.close();
			//patterns.close();

			String version = content.split("\"bugsVersion\":")[1];
			version = version.substring(0, version.length()-1); // Remove the "{"
			System.out.println("Version of bugs: " + version);
			System.out.println("Number of elements parsed: " + count);
		}
		catch (FileNotFoundException e) {
			System.out.println("File not found (bugs.json is required in the same folder).");
		}
	}
}
