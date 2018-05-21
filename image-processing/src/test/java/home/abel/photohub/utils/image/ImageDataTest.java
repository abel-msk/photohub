package home.abel.photohub.utils.image;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

//@RunWith(SpringJUnit4ClassRunner.class)
//@PropertySource("classpath:unittest.properties") // does not override ExtractionBatchConfiguration declaration



public class ImageDataTest {

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
        metadata.setCamMake("Canon");

        File outFile = new File("/tmp/updmeta.jpg");
        outFile.createNewFile();
        imgObj.saveJPEG(new FileOutputStream(outFile));

        imgObj = new ImageData(new FileInputStream(outFile));
        metadata = imgObj.getMetadata();

        assertThat(metadata.getDateCreated()).isEqualToIgnoringMillis(currentDate);
        assertThat(metadata.getDateOriginal()).isEqualToIgnoringMillis(currentDate);
        assertThat(metadata.getDateUpdate()).isEqualToIgnoringMillis(currentDate);
        assertThat(metadata.getCamMake()).isEqualToIgnoringCase("Canon");

    }




}
