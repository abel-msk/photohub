package home.abel.photohub.web.model;

import java.io.Serializable;

import home.abel.photohub.service.ConfVarEnum;

public class ResponseConfigParamObject implements Serializable {
	private static final long serialVersionUID = 1L;

    private  String name;   
    private  String access; 	
    private  String type;
    private  String value;
    private  int sort;
    
	public ResponseConfigParamObject() {
		
	}    
    /*
	public ResponseConfigParamObject(String name) {
		this.name = name;
	}
    
	public ResponseConfigParamObject(ConfVarEnum  theVarEnum) {
		this.name = theVarEnum.getName();
		this.access = theVarEnum.getAccess();
		this.type  = theVarEnum.getType();
	}
	*/
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAccess() {
		return access;
	}

	public void setAccess(String access) {
		this.access = access;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public int getSort() {
		return sort;
	}

	public void setSort(int sort) {
		this.sort = sort;
	}	
}
