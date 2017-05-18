package home.abel.photohub.connector.google;

public class ExceptionNotAuthorized extends Exception {
	private static final long serialVersionUID = 1L;

	ExceptionNotAuthorized() {
		super("Access to Google not authorized.");
	}
	
	ExceptionNotAuthorized(String Message) {
		super(Message);
	}
}
