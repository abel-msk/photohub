package home.abel.photohub.web.model;
import java.io.Serializable;

public class DefaultResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private String message;
	private Integer rc;
	
	public DefaultResponse() {
		
	}
	public DefaultResponse (String msg, Integer retCode) {
		message = msg;
		rc = retCode;
	}
	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}
	/**
	 * @param message the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}
	/**
	 * @return the rc
	 */
	public Integer getRc() {
		return rc;
	}
	/**
	 * @param rc the rc to set
	 */
	public void setRc(Integer rc) {
		this.rc = rc;
	}
	
	public String toString() {
		return "rc="+rc+", msg="+message;
	}
}
