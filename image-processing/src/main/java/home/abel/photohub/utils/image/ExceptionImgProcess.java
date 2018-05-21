package home.abel.photohub.utils.image;

public class ExceptionImgProcess extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ExceptionImgProcess(String message) {
		super(message);
	}
	
	public ExceptionImgProcess(String message, Exception e) {
		super(message,e);
	}
}
