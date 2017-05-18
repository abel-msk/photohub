package home.abel.photohub.connector.google;

public class ExceptionBreakAuthFlow extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ExceptionBreakAuthFlow(String message) {
		super(message);
	}
	public ExceptionBreakAuthFlow(String message,Throwable e) {
		super(message,e);
	}
}
