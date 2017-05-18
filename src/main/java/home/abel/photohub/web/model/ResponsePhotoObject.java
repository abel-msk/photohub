package home.abel.photohub.web.model;


import java.io.Serializable;

public class ResponsePhotoObject implements Serializable  {
	private static final long serialVersionUID = 1L; 
	
	private String id;
	private String parentId;
	private String siteId;
	private String thumbUrl;
	private String photoUrl;
	private String name;
	private String descr;
	private String type;
	private String createDate;
	private String modDate;
	private ResponseObjectsListPage<ResponsePhotoObject>  listByPage = null;	
	private Iterable<ResponsePhotoObject>  list = null;
	
	public ResponseObjectsListPage<ResponsePhotoObject> getListByPage() {
		return listByPage;
	}

	public void setListByPage(ResponseObjectsListPage<ResponsePhotoObject> listByPage) {
		this.listByPage = listByPage;
	}

	public Iterable<ResponsePhotoObject> getList() {
		return list;
	}
	
	public void setList(Iterable<ResponsePhotoObject> list) {
		this.list = list;
	}
	
	public ResponsePhotoObject() {

	}
	
	public void setId( String theId) {
		id = theId;
	}
	
	public void setParentId( String pi) {
		parentId = pi;
	}

	public void setThumbUrl( String tu) {
		thumbUrl = tu;
	}
	
	public void setPhotoUrl(String pu) {
		photoUrl = pu;
	}

	public void setType(String theType) {
		type = theType;
	}
	
	public String getId() {
		return id;
	}
	public String getParentId() {
		return parentId;
	}
	public String getThumbUrl() {
		return thumbUrl;
	}
	public String getPhotoUrl() {
		return photoUrl;
	}
	
	public String getType() {
		return type;
	}
	
	//   NAME
	public String getName() {
		return name;
	}
	
	public void setName(String theName) {
		name = theName;
	}
	
	//   DESCR
	public String getDescr() {
		return descr;
	}
	
	public void setDescr(String theDescr) {
		descr = theDescr;
	}	
	
	//  SITEID
	public String getSiteId() {
		return siteId;
	}
	
	public void setSiteId(String theSite) {
		siteId = theSite;
	}

	/**
	 * @return the createDate
	 */
	public String getCreateDate() {
		return createDate;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(String createDate) {
		this.createDate = createDate;
	}

	/**
	 * @return the modDate
	 */
	public String getModDate() {
		return modDate;
	}

	/**
	 * @param modDate the modDate to set
	 */
	public void setModDate(String modDate) {
		this.modDate = modDate;
	}		
}
