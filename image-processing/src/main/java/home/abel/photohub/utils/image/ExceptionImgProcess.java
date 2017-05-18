package home.abel.photohub.utils.image;

public class ExceptionImgProcess extends Exception {

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
