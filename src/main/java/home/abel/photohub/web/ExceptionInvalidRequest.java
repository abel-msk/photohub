package home.abel.photohub.web;

public class ExceptionInvalidRequest extends Exception{
	private static final long serialVersionUID = 1L;
	
	public ExceptionInvalidRequest (String message){
		super(message);
	}
	
	public ExceptionInvalidRequest(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionInvalidRequest(String message, Throwable cause) {
		super(message, cause);
	}	
	
}
