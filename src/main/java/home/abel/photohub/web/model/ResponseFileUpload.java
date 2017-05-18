package home.abel.photohub.web.model;

import java.io.Serializable;

public class ResponseFileUpload extends DefaultResponse implements Serializable {
	private static final long serialVersionUID = 1L;
	private String filename;
	ResponsePhotoObject object = null;
	String nodeId = "0";
	
	public ResponseFileUpload(String msg, String fn, String nId, Integer retcode) {
		super(msg,retcode);
		filename = fn;
		nodeId = nId;
	}

	public ResponseFileUpload(String msg, String fn, Integer retcode) {
		super(msg,retcode);
		filename = fn;
	}
	
	public String getNodeId() {
		return nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * @return the filename
	 */
	public String getFilename() {
		return filename;
	}
	/**
	 * @param filename the filename to set
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	public ResponsePhotoObject getObject() {
		return object;
	}
	public void setObject(ResponsePhotoObject object) {
		this.object = object;
	}

	
	
	
}
