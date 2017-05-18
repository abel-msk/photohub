package home.abel.photohub.connector;

import java.io.Serializable;

import home.abel.photohub.connector.prototype.SitePropertyInt;

public class SiteBaseProperty implements SitePropertyInt, Serializable  {
	protected static final long serialVersionUID = 1L;
	protected String name = null;
	protected String descr = null;
	protected String value = null;
	protected boolean updatable = true;
	
	public SiteBaseProperty(String name) {
		this.name = name;
		this.descr = "";
	}
	
	public SiteBaseProperty(String name, String value) {
		this.name = name;

		this.value = value;
	}
	public SiteBaseProperty(String name, String descr, String value) {
		this.name = name;
		this.descr = descr;
		this.value = value;
	}
	
	public SiteBaseProperty(String name, String descr, String value, boolean updatable) {
		this.name = name;
		this.descr = descr;
		this.value = value;
		this.updatable = updatable;
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	public SitePropertyInt setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean isUpdatable() {
		return updatable;
	}
	
	@Override
	public String getDescr() {
		return descr;
	}

	public void setDescr(String descr) {
		this.descr = descr;
	}


	@Override
	public SitePropertyInt setValue(String theValue) {
		this.value = theValue;
		return null;
	}

	@Override
	public SitePropertyInt setValueObj(SitePropertyInt theValueObj) {
		this.value = theValueObj.getValue();
		return null;
	}
	
	@Override
	public String getValue() {
		return this.value;
	}

	@Override
	public SitePropertyInt clone() {
		return new SiteBaseProperty(
				this.name == null? null:new String(this.name),
				this.descr== null? null:new String(this.descr),
				this.value== null? null:new String(this.value));
	}
}
