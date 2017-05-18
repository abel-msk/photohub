package home.abel.photohub.service;

public class ExceptionInternalError extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	public ExceptionInternalError (String message){
		super(message);
	}
	
	public ExceptionInternalError(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionInternalError(String message, Throwable cause) {
		super(message, cause);
	}	

}
