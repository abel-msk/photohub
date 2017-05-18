package home.abel.photohub.connector.prototype;

public class ExceptionIncorrectParams extends RuntimeException {
	private static final long serialVersionUID = 1L;

	
	public ExceptionIncorrectParams() {
	}

	public ExceptionIncorrectParams(String message) {
		super(message);
	}

	public ExceptionIncorrectParams(Throwable cause) {
		super(cause);
	}

	public ExceptionIncorrectParams(String message, Throwable cause) {
		super(message, cause);
	}

	public ExceptionIncorrectParams(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
