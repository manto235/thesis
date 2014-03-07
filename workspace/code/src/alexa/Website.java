package alexa;

/**
 * Object representing a website.
 *
 */
public class Website {
	private int position;
	private String url;

	/**
	 * Constructor.
	 *
	 * @param position the website's position in the Alexa Top file.
	 * @param url the website's url.
	 */
	public Website(int position, String url) {
		this.position = position;
		this.url = url;
	}

	/**
	 * Gets the position of the website.
	 *
	 * @return an integer containing the position.
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * Get the url of the website.
	 *
	 * @return a String containing the url.
	 */
	public String getUrl() {
		return url;
	}
}
