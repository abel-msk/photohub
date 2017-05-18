package home.abel.photohub.connector.local;

import home.abel.photohub.connector.prototype.AccessException;
import home.abel.photohub.connector.prototype.ImageProcessingException;

import java.awt.image.BufferedImage;
import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ImageScaler {
	final Logger logger = LoggerFactory.getLogger(ImageScaler.class);
	
	/**
	 *  Read image as buffered image, pass to scale down and return inputstream to scaled image
	 * @param imageSource
	 * @param imgSize
	 * @return
	 * @throws Exception
	 */
	public InputStream doScale(File imageSource,  Dimension imgSize) throws IOException {
		try {
			BufferedImage inputImg = ImageIO.read(imageSource);
			return doScale(inputImg,imgSize);
		} catch (IOException e) {
			logger.error("Write image: " + e.getMessage());
			throw e;
		}
	}
	
	/**
	 * Scale down  the image and return inputstream to scaled result
	 * @param inputImg
	 * @param imgSize
	 * @return
	 * @throws ImageProcessingException
	 * @throws AccessException
	 */
	public InputStream doScale(BufferedImage inputImg,  Dimension imgSize)
			throws IOException {
		
		InputStream is = null;

		try {						
			//***   Scale Image
			logger.debug("Image scalling. FIT_TO_WIDTH=" + imgSize.width);
			BufferedImage imgToSave;
			if (inputImg.getWidth() > imgSize.width) {
	
				//http://www.thebuzzmedia.com/downloads/software/imgscalr/javadoc/org/imgscalr/Scalr.html
				imgToSave =   Scalr.resize(inputImg, Scalr.Method.QUALITY, Scalr.Mode.FIT_TO_WIDTH,
						imgSize.width);
				//Scalr.OP_ANTIALIAS
			}
			else {
				logger.debug("Image input image less than required width, so skip scaling.");
				imgToSave = inputImg;
			}		

			ByteArrayOutputStream os = new ByteArrayOutputStream();
			ImageIO.write(imgToSave, "png", os);
			is = new ByteArrayInputStream(os.toByteArray());
		
		} catch (IOException e) {
			logger.error("Write image: " + e.getMessage());
			throw e;
		}

		return is;
	}

}
