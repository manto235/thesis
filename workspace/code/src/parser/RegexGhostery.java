package parser;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RegexGhostery {
	private Map<String, String> regex;
	private int bugsVersion;
	private boolean success;

	public RegexGhostery() {
		regex = new HashMap<String, String>();
		loadTrackers();
	}

	public Map<String, String> getRegex() {
		return regex;
	}

	public int getBugsVersion() {
		return bugsVersion;
	}

	public boolean isSuccess() {
		return success;
	}

	private void loadTrackers() {
		try {
			URL url = new URL("https://www.ghostery.com/update/bugs?format=json");
			ObjectMapper mapper = new ObjectMapper();
			JsonNode rootNode = mapper.readTree(url);

			Iterator<JsonNode> bugsElements = rootNode.get("bugs").iterator();
			while (bugsElements.hasNext()) {
				JsonNode bug = bugsElements.next();
				String pattern = bug.get("pattern").asText().replace("\\", "");
				String name = bug.get("name").asText();
				regex.put(pattern, name);
			}

			success = true;
			bugsVersion = rootNode.get("bugsVersion").intValue();
		}
		catch (IOException e) {
			success = false;
		}
	}
}
