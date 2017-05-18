package home.abel.photohub.service.auth;

public class ExceptionAccessDeny extends Exception {
	private static final long serialVersionUID = 1L;
	
	public ExceptionAccessDeny(String message) {
		super(message);
	}
	public ExceptionAccessDeny(String message, Throwable e) {
		super(message,e);
	}
}
