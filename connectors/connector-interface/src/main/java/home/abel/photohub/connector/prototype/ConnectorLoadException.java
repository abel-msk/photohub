package home.abel.photohub.connector.prototype;

public class ConnectorLoadException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ConnectorLoadException(String message) {
		super(message);
	}
	
	public ConnectorLoadException(String message, Exception e) {
		super(message, e);
	}
	
	public ConnectorLoadException(Exception e) {
		super(e.getMessage(), e);
	}
}
