package home.abel.photohub.service;

public class ExceptionDBContent extends RuntimeException {
	private static final long serialVersionUID = 1L;

	
	public ExceptionDBContent (String message){
		super(message);
	}
	
	public ExceptionDBContent(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionDBContent(String message, Throwable cause) {
		super(message, cause);
	}
	
	
}
