package home.abel.photohub.service;

public class ExceptionPhotoProcess extends java.lang.RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ExceptionPhotoProcess (String message){
		super(message);
	}
	
	public ExceptionPhotoProcess(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionPhotoProcess(String message, Throwable cause) {
		super(message, cause);
	}	
}
