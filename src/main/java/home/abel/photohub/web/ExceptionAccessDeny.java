package home.abel.photohub.web;

public class ExceptionAccessDeny extends Exception {
	private static final long serialVersionUID = 1L;
	
	public ExceptionAccessDeny (String message){
		super(message);
	}
	
	public ExceptionAccessDeny(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionAccessDeny(String message, Throwable cause) {
		super(message, cause);
	}	
	
	
	
	
}
