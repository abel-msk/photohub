package home.abel.photohub.utils.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import javax.imageio.*;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.sun.imageio.plugins.jpeg.JPEGMetadata;
import com.sun.imageio.plugins.png.PNGMetadata;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.write.TiffImageWriterLossless;
import org.apache.commons.imaging.formats.tiff.write.TiffOutputSet;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




// https://github.com/najiji/photoTag/blob/master/src/com/najiji/photoTag/ExifManager.java
/*
		    printTagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_XRESOLUTION);
		    printTagValue(jpegMetadata, TiffTagConstants.TIFF_TAG_DATE_TIME);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_ORIGINAL);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_DATE_TIME_DIGITIZED);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_ISO);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_SHUTTER_SPEED_VALUE);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_APERTURE_VALUE);
		    printTagValue(jpegMetadata, ExifTagConstants.EXIF_TAG_BRIGHTNESS_VALUE);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE_REF);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LATITUDE);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE_REF);
		    printTagValue(jpegMetadata, GpsTagConstants.GPS_TAG_GPS_LONGITUDE);



https://stackoverflow.com/questions/20895911/how-to-embed-icc-profile-in-tiffoutputset

http://www.silverbaytech.com/2014/06/04/iiometadata-tutorial-part-3-writing-metadata/

https://github.com/cpesch/RouteConverter/blob/master/navigation-formats/src/main/java/slash/navigation/photo/PhotoFormat.java


https://github.com/svn2github/sanselanandroid/blob/master/SanselanAndroid/src/org/apache/sanselan/formats/tiff/constants/ExifTagConstants.java

https://stackoverflow.com/questions/8972357/manipulate-an-image-without-deleting-its-exif-data

 */

public class ImageData {
    final static Logger logger = LoggerFactory.getLogger(ImageData.class);


    BufferedImage image;
    TiffImageMetadata tiffMetadata = null;
    IIOMetadata metadata = null;
    Metadata theMetadataClass = null;


    public ImageData(InputStream instream) throws IOException {

        byte[] imageData = IOUtils.toByteArray(instream);
        instream.close();

        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData));
        ImageReader reader = ImageIO.getImageReaders(iis).next();

        try {
            tiffMetadata = readExifMetadata(imageData);
        }
        catch (ImageReadException | ImageWriteException ie) {
            logger.warn("[ImageData.init] cannot read metadata."+ ie.getLocalizedMessage());
        }
        image =  ImageIO.read(new ByteArrayInputStream(imageData));



//
//        ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData));
//
//        ImageReader reader = ImageIO.getImageReaders(iis).next();
//        if (reader == null) throw new ExceptionImgProcess("[ImageData.Init] No jpeg reader plugin found. ImageIO Lib.");
//
//        reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(imageData)));
//        image = reader.read(0);
//        metadata = reader.getImageMetadata(0);
//
//        // Dispose reader in finally block to avoid memory leaks
//        reader.dispose();
//        iis.close();

    }

    /**
     *  Return @link{Metadata} object with this image exif data
     * @return
     */
    public Metadata getMetadata() {
        if (( theMetadataClass == null)  && (tiffMetadata != null)) {
            theMetadataClass = new Metadata(tiffMetadata);
        }
        return theMetadataClass;
    }

    public void setMetadata(Metadata md) {
        theMetadataClass = md;
    }


    private TiffImageMetadata readExifMetadata(byte[] jpegData) throws ImageReadException, ImageWriteException, IOException {
        ImageMetadata imageMetadata = Imaging.getMetadata(jpegData);
        TiffImageMetadata tiffMetadata = null;
        if (imageMetadata instanceof JpegImageMetadata)
            tiffMetadata = ((JpegImageMetadata) imageMetadata).getExif();
        else if (imageMetadata instanceof TiffImageMetadata)
            tiffMetadata = (TiffImageMetadata) imageMetadata;
        return tiffMetadata;
    }

    private byte[] writeExifMetadata(TiffImageMetadata metadata, byte[] jpegData)
            throws ImageReadException, ImageWriteException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new ExifRewriter().updateExifMetadataLossless(jpegData, out, metadata.getOutputSet());
        out.close();
        return out.toByteArray();
    }


    public  void rotateCCW() {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage newImage = new BufferedImage(h, w, image.getType());
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                newImage.setRGB(y,x,image.getRGB(x,y));
        image = newImage;
    }

    public void  rotateCW()  {
        int         width  = image.getWidth();
        int         height = image.getHeight();
        BufferedImage   newImage = new BufferedImage( height, width, image.getType() );

        for( int i=0 ; i < width ; i++ )
            for( int j=0 ; j < height ; j++ )
                newImage.setRGB( height-1-j, i, image.getRGB(i,j) );
        image = newImage;
    }


    public synchronized void resize(Dimension imgSize) throws ExceptionImgProcess {

        BufferedImage newImage =   Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH,
                imgSize.width);
        image = newImage;

    }

    /**
     *      Generate image in JPEG format. Printout to output stream.
     * @param os
     * @throws RuntimeException
     */
    public synchronized void saveJPEG(OutputStream os) throws RuntimeException {
        boolean canThrow = false;
        try {

            ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(jpegOut);

            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("JPEG");
            ImageWriter writer = iter.next();
            writer.setOutput(ios);

            ImageWriteParam iwParam = writer.getDefaultWriteParam();
            iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            iwParam.setCompressionQuality(.95f);

            writer.write(null, new IIOImage(image, null, null), iwParam);
            writer.dispose();
            ios.close();
            jpegOut.close();


            TiffOutputSet outputSet = null;
            if ( theMetadataClass != null ) {
                outputSet = theMetadataClass.saveOutputSet();
                logger.debug("[ImageData.saveJPEG] get outputset from metadata class.");
            }
            else if (tiffMetadata != null) {
                outputSet = tiffMetadata.getOutputSet();
                logger.debug("[ImageData.saveJPEG] get outputset from original metadata block.");
            }

            if ( outputSet != null) {
                new ExifRewriter().updateExifMetadataLossless(jpegOut.toByteArray(), os, outputSet);
            }
            else {
                IOUtils.write(jpegOut.toByteArray(), os);
            }

            //os.close();
            os.flush();

        } catch (Exception e) {
            throw new RuntimeException("Cannot save image data",e);
        }

        IOUtils.closeQuietly(os);
    }


    /**
     *      Generate image in PNG format. Printout to output stream.
     * @param os
     * @throws RuntimeException
     */
    public void savePNG(OutputStream os) throws RuntimeException {
        try {
            ImageOutputStream ios = ImageIO.createImageOutputStream(os);
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("PNG");
            ImageWriter writer = iter.next();
            writer.setOutput(ios);

//            ImageWriteParam iwParam = writer.getDefaultWriteParam();
//            iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//            iwParam.setCompressionQuality(.95f);
//

            // Create & populate metadata
            Calendar now = Calendar.getInstance();
            System.out.println(now.get(Calendar.HOUR_OF_DAY) + ":" + now.get(Calendar.MINUTE));

            PNGMetadata metadata = new PNGMetadata();  // see http://www.w3.org/TR/PNG-Chunks.html#C.tEXt for standardized keywords
            metadata.tEXt_keyword.add( "Title" );
            metadata.tEXt_text.add( "Mandelbrot" );
            metadata.tEXt_keyword.add( "Comment" );
            metadata.tEXt_text.add( "..." );

            metadata.tEXt_keyword.add( "Creation Time" );
            //metadata.tEXt_text.add( fractal.getCoords().toString() );

            metadata.tIME_day  = now.get(Calendar.DAY_OF_MONTH);
            metadata.tIME_month = now.get(Calendar.MONTH);
            metadata.tIME_year = now.get(Calendar.YEAR);
            metadata.tIME_hour = now.get(Calendar.HOUR_OF_DAY);
            metadata.tIME_minute = now.get(Calendar.MINUTE);
            metadata.tIME_second = now.get(Calendar.SECOND);

            writer.write(null, new IIOImage(image, null, metadata), null);
            writer.dispose();
            ios.close();
            os.close();

        } catch (Exception e) {
            throw new RuntimeException("Cannot save image data", e);
        }
    }


}