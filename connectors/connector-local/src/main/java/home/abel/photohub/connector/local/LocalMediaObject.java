package home.abel.photohub.connector.local;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;

import home.abel.photohub.connector.HeadersContainer;
import home.abel.photohub.connector.SiteMediaPipe;
import home.abel.photohub.utils.image.ImageData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import home.abel.photohub.connector.BaseMediaObject;
import home.abel.photohub.connector.prototype.EnumMediaType;
import home.abel.photohub.connector.prototype.SiteConnectorInt;

public class LocalMediaObject extends BaseMediaObject {
	final Logger logger = LoggerFactory.getLogger(LocalMediaObject.class);

	
	protected SiteConnectorInt connector = null;
//	protected File sourceFile = null;
//	protected String requestType = null;
	protected ImageData imageDataOld = null;

	
	public LocalMediaObject(SiteConnectorInt connector, ImageData imageDataOld) throws IOException {
		this.imageDataOld = imageDataOld;
//		sourceFile = source;
//		path = sourceFile.getAbsolutePath();
		this.connector = connector;
//		this.requestType =requestType;
		accType = EnumMediaType.ACC_LOACL;
	}

//	public InputStream getInputStream() throws IOException {
//
//		InputStream is = null;
//		if (sourceFile != null) {
//			if ( requestType.equalsIgnoreCase("THUMB")) {
//				//logger.trace("Scale image to width=" +getWidth()+", height="+getHeight());
//				ImageScaler scaller = new ImageScaler();
//				is = scaller.doScale(sourceFile, new Dimension(getWidth(),getHeight()));
//			} else {
//				is = new FileInputStream(sourceFile);
//			}
//		}
//		else {
//			logger.warn("Access to media object when source file not defined");
//		}
//		return is;
//	}

	@Override
	public SiteMediaPipe getContentStream(HeadersContainer headers) throws Exception {
		return this.getContentStream();
	}

	@Override
	public SiteMediaPipe getContentStream() throws Exception {

		InputStream is = null;

		if (imageDataOld != null) {
			ImageData newImage = null;
			if ((getWidth() != imageDataOld.getWidth()) || (getHeight() != imageDataOld.getHeight())) {
				newImage =  imageDataOld.resize(new Dimension(getWidth(),getHeight()));
				//  Redefine object size
				setHeight(newImage.getHeight());
				setWidth(newImage.getHeight());
			}
			else {
				newImage  = imageDataOld;
			}

			if (getMimeType().endsWith("png")) {
				is = newImage.savePNG();
			}
			else  if (getMimeType().endsWith("jpeg")) {
				is = newImage.saveJPEG();
			}
			//TODO: make save to TIFF
			else {
				logger.warn("Unsupported output image mime type  " + getMimeType() +",  for image " + getPath());
			}
		}
		else {
			logger.warn("Access to media object when source file not defined.");
		}

		SiteMediaPipe pipe = new SiteMediaPipe();
		pipe.setInputStream(is);
		return pipe;
	}





}
