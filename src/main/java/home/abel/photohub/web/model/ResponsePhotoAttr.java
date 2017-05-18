package home.abel.photohub.web.model;

import home.abel.photohub.service.PhotoAttrEnum;

import java.io.Serializable;

public class ResponsePhotoAttr implements Serializable {
	private static final long serialVersionUID = 1L;
		
	private String name = null;
	private String value = null;
	private String type = null;
	private String access = null;
	private String namespace = null;
	private String displayName = null;
		

	public ResponsePhotoAttr(PhotoAttrEnum attr, String value) {
		this(attr);
		setValue(value);
	}

	public ResponsePhotoAttr(String theAttrName, String value) {
		this(PhotoAttrEnum.fromName(theAttrName));
		setValue(value);
	}	
	
	
	public ResponsePhotoAttr(PhotoAttrEnum attr) {
		setName(attr.toString());
		setType(attr.getType());
		setAccess(attr.getAccess());
		setDisplayName(attr.getDisplayName());
		setNamespace(attr.getNamespace());
	}
	
	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
}
