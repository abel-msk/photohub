package home.abel.photohub.service.auth;

public class ExceptionIncorrectFormat extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ExceptionIncorrectFormat(String message) {
		super(message);
	}
	public ExceptionIncorrectFormat(String message, Throwable e) {
		super(message,e);
	}
}
