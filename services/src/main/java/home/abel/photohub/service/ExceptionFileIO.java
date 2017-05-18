package home.abel.photohub.service;

public class ExceptionFileIO extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public ExceptionFileIO (String message){
		super(message);
	}
	
	public ExceptionFileIO(Throwable cause) {
		super("Nested from: ",cause);
	}
	
	public ExceptionFileIO(String message, Throwable cause) {
		super(message, cause);
	}
}
