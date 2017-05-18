package home.abel.photohub.tasks;

public class ExceptionTaskAbort extends RuntimeException {
	public ExceptionTaskAbort(String message) {
		super(message);
	}
	public ExceptionTaskAbort(String message,Throwable origException) {
		super(message,origException);
	}
}
