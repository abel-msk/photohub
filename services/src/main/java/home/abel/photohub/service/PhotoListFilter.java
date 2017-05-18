package home.abel.photohub.service;

import home.abel.photohub.model.QPhoto;
import home.abel.photohub.utils.InternetDateFormat;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.querydsl.core.types.dsl.BooleanExpression;

public class PhotoListFilter implements Serializable {
	final Logger logger = LoggerFactory.getLogger(PhotoListFilter.class);
	private static final long serialVersionUID = -2089922880123271431L;
	
	private Date minDate = null;
	private Date maxDate   = null;
	private List<String> sites = null;
//	private double gpsLatitude;
//	private double gpsLongitude;
//	private int radius = 100000;
	
	
	public Date getMinDate() {
		return minDate;
	}
	public void setMinDate(Date minDate) {
		this.minDate = minDate;
	}
	public void setMinDate(String minDateStr) throws RuntimeException {
		if ((minDateStr == null) || (minDateStr.length() == 0)) {
			this.minDate = null;  //  Clear current value
			return;
		}
		InternetDateFormat dateConvertor = new InternetDateFormat();
		try {
			this.minDate = dateConvertor.parse(minDateStr);
		} catch (ParseException e) {
			throw new ExceptionInvalidArgument("Incorrect min date parameter " + minDateStr,e);
		}
	}
	
	public Date getMaxDate() {
		return maxDate;
	}
	
	public void setMaxDate(Date maxDate) {
		this.maxDate = maxDate;
	}
	
	public void setMaxDate(String maxDateStr) {
		if ((maxDateStr == null) || (maxDateStr.length() == 0)) {
			this.maxDate = null;  //  Clear current value
			return;
		}
		InternetDateFormat dateConvertor = new InternetDateFormat();
		try {
			this.maxDate = dateConvertor.parse(maxDateStr);
		} catch (ParseException e) {
			throw new ExceptionInvalidArgument("Incorrect max date parameter " + maxDateStr,e);
		}
	}
	
	public List<String> getSites() {
		return sites;
	}
	public void setSites(List<String> sites) {
		if (sites.size() > 0 )  this.sites = sites;
		else this.sites=null;
	}
	
	@JsonIgnore
	public  BooleanExpression getCriteria() {
		
		BooleanExpression sqlFilter = null;
				
		//  Устанавливаем нижний порог даты
		if (getMinDate() != null) {
			sqlFilter = QPhoto.photo.createTime.gt(getMinDate());
		}
		
		//  Устанавливаем верхний порог даты
		if (getMaxDate() != null) {
			if ( sqlFilter != null) {
				sqlFilter = sqlFilter.and(QPhoto.photo.createTime.lt(getMaxDate()));
			}
			else {
				sqlFilter = QPhoto.photo.createTime.lt(getMaxDate());
			}
		}
		
		//   Добавляем  выборку по Сайтам
		if ( getSites() != null) {
			for ( String siteId: getSites()) {
				if ( sqlFilter == null) {
					sqlFilter = QPhoto.photo.siteBean.id.eq(siteId);
				}
				else {
					sqlFilter = sqlFilter.or(QPhoto.photo.siteBean.id.eq(siteId));
				}
			}
		}
		logger.debug("Create request filter :"+(sqlFilter!=null?sqlFilter.toString():"empty"));
		return sqlFilter; 
	}
	

	
}
