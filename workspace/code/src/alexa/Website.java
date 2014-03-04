package alexa;

public class Website {
	private int position;
	private String url;
	
	public Website(int position, String url) {
		this.position = position;
		this.url = url;
	}
	
	public int getPosition() {
		return position;
	}
	
	public String getUrl() {
		return url;
	}
}
