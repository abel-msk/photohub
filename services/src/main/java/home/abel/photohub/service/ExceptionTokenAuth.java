package home.abel.photohub.service;

public class ExceptionTokenAuth extends Exception {
	private static final long serialVersionUID = 1L;
	
	public ExceptionTokenAuth(String message) {
		super(message);
	}
	public ExceptionTokenAuth(String message, Throwable e) {
		super(message,e);
	}	
}
