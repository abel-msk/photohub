package home.abel.photohub.utils.image;

import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;


public class ImageScaler {
	final static Logger logger = LoggerFactory.getLogger(ImageScaler.class);

	public static String doScale(String fromFileName, String toFileName, Dimension imgSize)
			throws ExceptionImgProcess, ExceptionImgAccess {
		
		logger.debug("doScale: from " + fromFileName + ",  to " + toFileName);		
		BufferedImage inputImg = null;
	
		try {

			//***   OPEN image From URL
			if (fromFileName.startsWith("http://") || (fromFileName.startsWith("https://"))) {
				URL imageURL;
				imageURL = new URL(fromFileName);
				logger.info("Load image from URL  " + imageURL.toString());				
				inputImg = ImageIO.read(imageURL);				
			}
			//***   OPEN image From File
			else {
				File inputFile = null;			
				if ( fromFileName.startsWith("file://") ) {
					inputFile = new File(fromFileName.substring(7));
				} else {
					inputFile = new File(fromFileName);
				}
				
				logger.info("Read image from file " + inputFile.getAbsolutePath());
				if ( ! inputFile.canRead())  {
					logger.debug("Cannot read input file: " + inputFile.getPath());
				}
				if ( ! inputFile.exists()) {
					logger.debug("Does not exist input file: " + inputFile.getPath());
				}
				
				inputImg = ImageIO.read(inputFile);
			}
			if (inputImg == null) {
				logger.error("The file is not an image file.");
				throw new ExceptionImgAccess("The file is not an image file.");		
			}
		} 
		catch (Exception e) {
			logger.error("Read image: " + e.getLocalizedMessage());
			throw new ExceptionImgAccess(e.getLocalizedMessage());
		}
				
		//***   Create File object for save thumbnail
		File outFile = new File(toFileName);
		
		//***   Check and create dirctory for new file
		try {
			FileUtils.forceMkdir(outFile.getParentFile());
		} catch (IOException e1) {
			String errMsg = "Cannot create directry " + outFile.getParent() + " : " + e1.getLocalizedMessage();
			logger.error(errMsg);
			throw new ExceptionImgAccess(errMsg,e1);
		}	
		
		//***   Scale Image
		logger.debug("Image scalling. FIT_TO_WIDTH=" + imgSize.width);
		BufferedImage imgToSave;
		if (inputImg.getWidth() > imgSize.width) {

			//http://www.thebuzzmedia.com/downloads/software/imgscalr/javadoc/org/imgscalr/Scalr.html
			imgToSave =   Scalr.resize(inputImg, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH,
					imgSize.width);
			//Scalr.OP_ANTIALIAS
		}
		else {
			logger.debug("Image input image less than required width, so skip scaling.");
			imgToSave = inputImg;
		}		
		
		//***   Save image to file
		try {
			ImageIO.write(imgToSave, "png", outFile);
		} catch (IOException e) {
			logger.error("Write image: " + e.getMessage());
			throw new ExceptionImgProcess(e.getMessage());
		}

		return outFile.getAbsolutePath();
	}

}
