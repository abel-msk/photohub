package home.abel.photohub.connector.prototype;

public class AccessException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public AccessException(String message) {
		super(message);
	}
	
	public AccessException(String message, Exception e) {
		super(message, e);
	}
	
	public AccessException(Exception e) {
		super(e.getMessage(), e);
	}
}
