package home.abel.photohub.utils.image;

import org.apache.commons.imaging.*;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.jpeg.JpegPhotoshopMetadata;
import org.apache.commons.imaging.formats.jpeg.exif.ExifRewriter;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcRecord;
import org.apache.commons.imaging.formats.jpeg.iptc.IptcTypes;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.TiffConstants;
import org.apache.commons.imaging.formats.tiff.write.*;
import org.apache.commons.imaging.util.Debug;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ImageData {
    final static Logger logger = LoggerFactory.getLogger(ImageData.class);

    private static String[] VALID_IMAGE_EXT_ARRAY = {"gif", "tiff", "tif", "jpg", "jpeg", "png"};
    private static String[] VALID_META_EXT_AR = {"tiff", "tif", "jpg", "jpeg"};

    private byte[] imageData = null;
    private TiffImageMetadata tiffMetadata = null;
    private long size = 0;
    private String  srcFormat = "unknown";
    private int height = 0;
    private int width = 0;
    private Metadata theMetadataClass = null;
    private boolean readOnly = false;

     /*--------------------------------------------------------------------------------------------

        READING AND LOADING

     --------------------------------------------------------------------------------------------*/

    /**
     *
     *    Load image from  input stream
     *
     * @param instream
     * @throws ExceptionIncorrectImgFormat
     */
    public ImageData(InputStream instream) throws ExceptionIncorrectImgFormat {
        try {
            load(IOUtils.toByteArray(instream));
            IOUtils.closeQuietly(instream);
        } catch (IOException e ) {
            throw new ExceptionIncorrectImgFormat("Unknown image format.",e);
        }
        try {
            tiffMetadata = readExifMetadata(imageData);
        } catch (Exception e) {}

    }

    /**
     *
     *     Construct class from image from memory
     *
     * @param imageData
     * @param meta
     * @throws ExceptionIncorrectImgFormat
     */
    public ImageData(byte[] imageData, TiffImageMetadata meta) throws ExceptionIncorrectImgFormat {
        try {
            load(imageData);
        } catch (IOException e ) {
            throw new ExceptionIncorrectImgFormat("Unknown image format.",e);
        }

        tiffMetadata = meta;
        if ( meta == null )
            try {
                tiffMetadata = readExifMetadata(imageData);
            } catch (Exception e) {}
    }

    private void load(byte[] imageData) throws IOException {
        this.imageData = imageData;  //TODO: Clone Image Data

        getImageProperties(imageData);

        try {
            tiffMetadata = readExifMetadata(imageData);
        }
        catch (ImageReadException | ImageWriteException ie) {
            logger.warn("[ImageData.init] cannot read metadata."+ ie.getLocalizedMessage());
        }
    }


    private void getImageProperties(byte[] imageBytes) throws ExceptionIncorrectImgFormat {
        ImageReader reader = null;
        ImageInputStream iis = null;

        try {
            size = imageBytes.length;
            iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageBytes));
            Iterator iter = ImageIO.getImageReaders(iis);
            if (!iter.hasNext()) throw new ExceptionIncorrectImgFormat("Unknown image format.");
            reader = (ImageReader) iter.next();
            if ( reader == null ) {
                throw new ExceptionIncorrectImgFormat("Cannot find reader fro input stream");
            }
            reader.setInput(iis);
            srcFormat = reader.getFormatName();

            //logger.debug("Image has "+ reader.getNumImages(true) + " images");

        } catch (RuntimeException | IOException e ) {
            if ( reader != null) reader.dispose();
            IOUtils.closeQuietly(iis);
            throw new ExceptionIncorrectImgFormat("[getImageProperties] Cannot get reader for input image.");
        }

        try {
            height = reader.getHeight(0);
            width = reader.getWidth(0);
        }
        catch ( Exception e ) {
            logger.error("[getImageProperties]  Cannot get image dimension for format "+ srcFormat, e);
        }
        finally {
            reader.dispose();
            IOUtils.closeQuietly(iis);
        }
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

    public boolean isHasMetadata() {
        return tiffMetadata != null;
    }


    /*--------------------------------------------------------------------------------------------

        GETTERS AND SETTERS

     --------------------------------------------------------------------------------------------*/


    /**
     *
     *      Return @link{Metadata} object with this image exif data
     *
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

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getSrcFormat() {
        return srcFormat;
    }

    public void setSrcFormat(String srcFormat) {
        this.srcFormat = srcFormat;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }


    /*--------------------------------------------------------------------------------------------

        SAVING

     --------------------------------------------------------------------------------------------*/

    /**
     *
     *    Build in memory image in requited image format input format from raster
     *
     * @param image source raster
     * @param outFormat  th result image format
     * @return bytes array with image wo metadata
     * @throws RuntimeException
     */
    public byte[] compileImage(BufferedImage image, String outFormat ) throws RuntimeException {

        ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();

        try {
            ImageOutputStream ios = ImageIO.createImageOutputStream(jpegOut);
            Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName(outFormat);
            ImageWriter writer = iter.next();
            writer.setOutput(ios);
            ImageWriteParam iwParam = null;

            if (outFormat.toUpperCase().equals("JPEG")) {
                iwParam = writer.getDefaultWriteParam();
                iwParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                iwParam.setCompressionQuality(.95f);
            }

            writer.write(null, new IIOImage(image, null, null), iwParam);
            writer.dispose();
            ios.close();
            jpegOut.close();

        } catch (IOException e) {
            throw new ExceptionImgProcess("[compileWithMeta] cannot prepare image with format " + outFormat);
        }

        return jpegOut.toByteArray();
    }

    /**
     *
     *     Merge jpeg image and metadata  and send  to output stream
     *
     * @param os
     * @throws RuntimeException
     */
    public synchronized void saveJPEG(OutputStream os) throws RuntimeException, ExifRewriter.ExifOverflowException {

        if ( isReadOnly() ) throw new ExceptionImgProcess("Image marked as readonly");

        byte[] jpegImageBytes = imageData;

        //
        //   Source data need to be converted
        //   do it throught BufferedImage
        //
        if (  ! srcFormat.equalsIgnoreCase("JPEG")) {
            try {
                logger.debug("[saveJPEG] Do format convert, Source format = " + srcFormat);
                jpegImageBytes = compileImage(ImageIO.read(new ByteArrayInputStream(imageData)), "JPEG");
            }
            catch (IOException e) {
                IOUtils.closeQuietly(os);
                throw new ExceptionImgProcess("Cannot convert image to JPEG format.");
            }
        }

        try {
            //
            //      Prepare writable Metadata
            //
            TiffOutputSet outputSet = null;
            if ((theMetadataClass != null) && (theMetadataClass.isChanged())) {
                outputSet = theMetadataClass.saveOutputSet();
            } else if (tiffMetadata != null) {
                outputSet = tiffMetadata.getOutputSet();
            }

            //
            //      Merge Image  with metadata if exist
            //
            if (outputSet != null) {
                new ExifRewriter().updateExifMetadataLossless(jpegImageBytes, os, outputSet);
            } else {
                IOUtils.write(jpegImageBytes, os);
            }
        } catch (ExifRewriter.ExifOverflowException eOver) {
            logger.warn("[saveJPEG] "+ eOver.getMessage());
            throw eOver;
        } catch (IOException | ImageWriteException | ImageReadException e) {
            throw new ExceptionImgProcess("[compileWithMeta] Cannot Write image",e);
        }
        finally {
            IOUtils.closeQuietly(os);
        }
    }


    /**
     *
     *      Merge jpeg image and metadata, save in memory and attach to input stream
     *
     * @return
     * @throws RuntimeException
     */
    public synchronized  InputStream saveJPEG() throws RuntimeException, ExifRewriter.ExifOverflowException {
        ByteArrayOutputStream jpegOut = new ByteArrayOutputStream();
        saveJPEG(jpegOut);
        ByteArrayInputStream is = new ByteArrayInputStream(jpegOut.toByteArray());
        return is;
    }



    public void saveOverJPEG(OutputStream os) throws ImageWriteException{
        try {
            saveJPEG(os);
        }
        catch (ExifRewriter.ExifOverflowException e ) {
            Metadata md = getMetadata();
            md.setOutputSet(md.copyOutputSet());
            saveJPEG(os);
        }
    }


    /**
     *
     *      Send image as PNG to output stream
     *
     * @param os
     * @throws RuntimeException
     */
    public synchronized void savePNG(OutputStream os) throws RuntimeException {

        if ( isReadOnly() ) throw new ExceptionImgProcess("Image marked as readonly");

        byte[] jpegImageBytes = imageData;

        //
        //   Source data need to be converted
        //   do it throught BufferedImage
        //
        if (  ! srcFormat.equalsIgnoreCase("PNG")) {
            try {
                jpegImageBytes = compileImage(ImageIO.read(new ByteArrayInputStream(imageData)), "PNG");
            }
            catch (IOException e) {
                IOUtils.closeQuietly(os);
                throw new ExceptionImgProcess("Cannot convert image to JPEG format.");
            }
        }

        try {
            IOUtils.write(jpegImageBytes, os);
        } catch (IOException e ) {
            throw new ExceptionImgProcess("[compileWithMeta] Cannot Write image");
        }
        finally {
            IOUtils.closeQuietly(os);
        }
    }

    /**
     *    Save image as PNG im memory and attach to input stream
     * @return
     * @throws RuntimeException
     */
    public synchronized  InputStream savePNG() throws RuntimeException {
        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
        savePNG(pngOut);
        ByteArrayInputStream is = new ByteArrayInputStream(pngOut.toByteArray());
        return is;
    }


//    /**
//     *     Send image as TIFF to output stream
//     * @return
//     * @throws RuntimeException
//     */
//
//    public void saveTIFF(OutputStream os) {
//
//        TiffOutputSet outputSet = null;
//        try {
//            if ((theMetadataClass != null) && (theMetadataClass.isChanged())) {
//                outputSet = theMetadataClass.saveOutputSet();
//            } else if (tiffMetadata != null) {
//                outputSet = tiffMetadata.getOutputSet();
//            }
//        } catch (ImageWriteException ex) {
//            logger.warn("[saveTIFF]  Cannot create metadata." + ex.getMessage());
//            throw new ExceptionImgProcess("[compileWithMeta] Cannot create metadata",ex);
//        }
//
//        try {
//
//            logger.debug("[saveTIFF] generate buffered image.");
//
//            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
//
//            if ( outputSet != null ) {
//                logger.debug("[saveTIFF] Generate image bytes.");
//                byte[] imageBytes = Imaging.writeImageToBytes(image, ImageFormats.TIFF, new HashMap<>());
//                logger.debug("[saveTIFF] Append metadata.");
//                new TiffImageWriterLossless(imageBytes).write(os, outputSet);
//            }
//            else {
//                ImageIO.write(image, "TIFF", os);
//            }
//
//        } catch (Exception ex)  {
//            logger.error("[saveTIFF] Cannot write tiff image.",ex.getMessage());
//            throw new ExceptionImgProcess("[compileWithMeta] Cannot Write image",ex);
//        }
//    }
//
//
//    /**
//     *    Save image as PNG im memory and attach to input stream
//     * @return
//     * @throws RuntimeException
//     */
//    public synchronized  InputStream saveTIFF() throws RuntimeException {
//        ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
//        saveTIFF(pngOut);
//        ByteArrayInputStream is = new ByteArrayInputStream(pngOut.toByteArray());
//        return is;
//    }

    /*--------------------------------------------------------------------------------------------

        TRANSFORMATIONS

     --------------------------------------------------------------------------------------------*/

//    /**
//     *
//     *  Rotate image clockwise  90 degree
//     *
//     * @return  new ImageData with result image, metadata is copied
//     * @throws RuntimeException
//     */
//    public ImageData rotateCCW() throws RuntimeException  {
//
//        //
//        // Old rotate method do not return new ImageData object
//        //
//        ImageData result = null;
//        try {
//            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
//            int w = image.getWidth();
//            int h = image.getHeight();
//            BufferedImage newImage = new BufferedImage(h, w, image.getType());
//
//            for (int y = 0; y < h; y++)
//                for (int x = 0; x < w; x++) {
//                    newImage.setRGB(y, w - 1 - x, image.getRGB(x, y));
//                }
//
//            result = new ImageData(compileImage(newImage,srcFormat),tiffMetadata);
//        } catch (IOException e) {
//            logger.error("[rotateCCW] Image rotate error. ",e);
//            throw new ExceptionImgProcess("[rotateCCW] Image rotate error. ",e);
//        }
//        return result;
//    }


    /**
     *
     *      Rotate image counter clockwise  or counter clockwise
     *
     * @param isClockwise - cloclwise rotation if true, counter clockwise else
     * @return new ImageData with result image, metadata is copied
     * @throws RuntimeException
     */
    public ImageData rotate(boolean isClockwise)  throws RuntimeException {
        ImageData result = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            int width  = image.getWidth();
            int height = image.getHeight();
            BufferedImage newImage = new BufferedImage( height, width, image.getType() );

            for( int i=0 ; i < width ; i++ )
                for( int j=0 ; j < height ; j++ )
                    if (isClockwise )
                        newImage.setRGB(height-1-j, i, image.getRGB(i, j));
                    else
                        newImage.setRGB(j, width-1-i, image.getRGB(i, j));



            result = new ImageData(compileImage(newImage,srcFormat),tiffMetadata);
        } catch (IOException e) {
            logger.error("[rotateCW] Image rotate error. ",e);
            throw new ExceptionImgProcess("[rotateCW] Image rotate error. ",e);
        }
        return result;
    }

    /**
     *
     *      Resize image with original aspect ration.
     *      The resulting image will have as least one dimension equals to required
     *      other dimension may be less or equals to required
     *
     * @param imgSize  squire dimension where are needed insert new image
     * @return  new ImageData with result image, metadata is copied
     * @throws ExceptionImgProcess
     */
    public synchronized ImageData resize(Dimension imgSize) throws ExceptionImgProcess {
        ImageData result = null;
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));

            BufferedImage newImage = Scalr.resize(image, Scalr.Method.ULTRA_QUALITY, Scalr.Mode.FIT_TO_WIDTH,
                    imgSize.width);

            result = new ImageData(compileImage(newImage, srcFormat), tiffMetadata);

            logger.debug("[resize] Generate resized image."
                    +" Req_width=" + imgSize.getWidth()+", got_width="+result.getWidth()
                    +", Req_height=" + imgSize.getHeight()+", got_height="+result.getHeight()
            );

        } catch (IOException e) {
            throw new ExceptionImgProcess("[compileWithMeta] Image rotate error.",e);
        }
        return  result;
    }

    /*--------------------------------------------------------------------------------------------

        STATIC UTILS

     --------------------------------------------------------------------------------------------*/


    public static boolean isValidImage(File theFile) {
        for (String ext: VALID_IMAGE_EXT_ARRAY) {
            if (theFile.getName().toUpperCase().endsWith(ext.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isImgHasMeta(File theFile) {
        for (String ext: VALID_META_EXT_AR) {
            if (theFile.getName().toUpperCase().endsWith(ext.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    public static String getFileExtension(String fileName) {
        String fileExt = "";
        final int index = fileName.lastIndexOf('.');
        if (index >= 0)
            fileExt = fileName.substring(index);
        return  fileExt;
    }

    public static String getMimeTypeByExt(String fileExt) {
        if (fileExt != null ) {

            switch (fileExt.toLowerCase()) {
                case "tiff":
                case "tif":
                    return "image/tiff";
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "mp4":
                case "m4v":
                case "avi":
                    return "video/mp4";
                case "mkv":
                    return "video/x-matroska";
                case "mov":
                    return 	"video/quicktime";
                //case "avi":
                default:
                    return "unknown";
            }
        }
        return null;
    }


    /**
     *
     * @param source
     * @param dest
     * @throws IOException
     */
    public static void copyFileUsingChannel(File source, File dest) throws IOException {
        FileChannel sourceChannel = null;
        FileChannel destChannel = null;
        try {
            sourceChannel = new FileInputStream(source).getChannel();
            destChannel = new FileOutputStream(dest).getChannel();
            destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());
        }finally{
            sourceChannel.close();
            destChannel.close();
        }
    }




}
