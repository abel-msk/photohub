package home.abel.photohub.service;

public class ExceptionInvalidArgument extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ExceptionInvalidArgument (String message){
		super(message);
	}
	
	public ExceptionInvalidArgument(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionInvalidArgument(String message, Throwable cause) {
		super(message, cause);
	}	
}
