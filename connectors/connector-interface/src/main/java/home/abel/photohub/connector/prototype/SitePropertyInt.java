package home.abel.photohub.connector.prototype;

public interface SitePropertyInt {
	
	public SitePropertyInt clone();
	
	
	public String getName();
	public SitePropertyInt setName(String name);
	public String getDescr();
	public boolean isUpdatable();
	
	public String getValue();	
	public SitePropertyInt setValue(String theValue);
	public SitePropertyInt setValueObj(SitePropertyInt theValueObj);
	
	
}
