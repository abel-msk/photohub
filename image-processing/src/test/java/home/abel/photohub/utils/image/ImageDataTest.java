package home.abel.photohub.utils.image;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

//@RunWith(SpringJUnit4ClassRunner.class)
//@PropertySource("classpath:unittest.properties") // does not override ExtractionBatchConfiguration declaration



public class ImageDataTest {
    final Logger logger = LoggerFactory.getLogger(ImageDataTest.class);


    public  File imgFile = null;

    @Before
    public void loadImage() {
        ClassLoader classLoader = getClass().getClassLoader();
        imgFile = new File(classLoader.getResource("sample3.JPG").getFile());
    }

    @Test
    public void  RotateTest() throws Throwable {

        ImageData imgObj = new ImageData(new FileInputStream(imgFile));

        imgObj.rotateCCW();
        File outFile = new File("/tmp/imgCCW.jpg");
        outFile.createNewFile();
        imgObj.saveJPEG(new FileOutputStream(outFile));

        imgObj.rotateCW();
        outFile = new File("/tmp/imgCW.jpg");
        outFile.createNewFile();
        imgObj.saveJPEG(new FileOutputStream(outFile));
    }

    @Test
    public void IOMetadataTest() throws Throwable {
        ImageData imgObj = new ImageData(new FileInputStream(imgFile));
        Metadata metadata = imgObj.getMetadata();

//        System.out.println("Date Created :\t"+metadata.getDateCreated());
//        System.out.println("Date Original :\t"+metadata.getDateOriginal());
//        System.out.println("Date Updated :\t"+metadata.getDateUpdate());
//        System.out.println("Cam make :\t"+metadata.getCamMake());

        Date currentDate = new Date();
        metadata.setDateUpdate(currentDate);
        metadata.setDateCreated(currentDate);
        metadata.setDateOriginal(currentDate);
        metadata.setCameraMake("Canon");

        File outFile = new File("/tmp/updmeta.jpg");
        outFile.createNewFile();
        imgObj.saveJPEG(new FileOutputStream(outFile));

        imgObj = new ImageData(new FileInputStream(outFile));
        metadata = imgObj.getMetadata();

        assertThat(metadata.getDateCreated()).isEqualToIgnoringMillis(currentDate);
        assertThat(metadata.getDateOriginal()).isEqualToIgnoringMillis(currentDate);
        assertThat(metadata.getDateUpdate()).isEqualToIgnoringMillis(currentDate);
        assertThat(metadata.getCameraMake()).isEqualToIgnoringCase("Canon");

    }

    @Test
    public void savePngTest() throws Throwable {
        ImageData imgObj = new ImageData(new FileInputStream(imgFile));
        File outFile = new File("/tmp/pngconvert.png");
        imgObj.savePNG(new FileOutputStream(outFile));
    }


    @Test
    public void resizeTest() throws Throwable {
        ImageData imgObj = new ImageData(new FileInputStream(imgFile));
        imgObj = imgObj.resize(new Dimension(300,300));
        logger.debug("Scaled image size = " +  imgObj.getSize());
        File outFile = new File("/tmp/sized.png");
        imgObj.savePNG(new FileOutputStream(outFile));

        imgObj = new ImageData(new FileInputStream("/tmp/sized.png"));
        assertThat(imgObj.getHeight()).isLessThanOrEqualTo(300);
        assertThat(imgObj.getWidth()).isLessThanOrEqualTo(300);
        logger.debug("Rereaded Scaled image size = " +  imgObj.getSize());
    }

    @Test
    public void genUUIDTest() throws Throwable {
        ImageData imgObj = new ImageData(new FileInputStream(imgFile));
        Metadata md = imgObj.getMetadata();
        md.setUnicId(Metadata.generateUUID());
        logger.debug("Set UUID to "+md.getUnicId()+", len="+md.getUnicId().length());
        imgObj.setMetadata(md);
        imgObj.savePNG(new FileOutputStream("chuuid.jpeg"));

    }



}
