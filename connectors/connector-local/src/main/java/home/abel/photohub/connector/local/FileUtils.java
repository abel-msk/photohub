package home.abel.photohub.connector.local;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileUtils {
	final static Logger logger = LoggerFactory.getLogger(FileUtils.class);

	
	/**
	 * 
	 * @param newFileName
	 * @return
	 */
	public static String getUnicName(String newFileName) {
		File newFile = new File(newFileName);
		return  getUnicName(newFile).getAbsolutePath();
	}
	
	
	/**
	 * 
	 * @param newFile
	 * @return
	 */
	public static File getUnicName(File newFile) {
		File workFile = newFile;
		String parentDir = workFile.getParent();
		String fNameWoExt = FilenameUtils.removeExtension(workFile.getName());
		String fNameExt = FilenameUtils.getExtension(workFile.getName());
		Integer fCount = new Integer(0);
		
		while (workFile.exists() && (fCount <= 1000)) {
			logger.trace("Check for path = " + workFile.getAbsolutePath());
			fCount++;
			workFile = new File(parentDir + File.separator + fNameWoExt + fCount + (fNameExt.isEmpty()?"":FilenameUtils.EXTENSION_SEPARATOR_STR + fNameExt));
		}
		logger.trace("Found uniq file path = " + workFile.getAbsolutePath());
		return workFile;
	}

	
	public static void saveFile(InputStream is, File fileSaveTo) throws IOException {
		
		final int BUFFER_SIZE = 8096;

		if (! fileSaveTo.getParentFile().exists() ) {
			try {
				org.apache.commons.io.FileUtils.forceMkdir(fileSaveTo.getParentFile());
			} catch (Throwable e) {
				//fileSaveTo.getParentFile().mkdirs();
				throw new IOException("Cannot access to enclosure folder " + fileSaveTo.getParentFile()+ ". "+ e.getMessage(),e);
			}
		}
		
		FileOutputStream fos = new FileOutputStream(fileSaveTo);
		
        int totalRead = 0;
        int readCount = 0;
        byte b[] = new byte[BUFFER_SIZE];
        while ((readCount = is.read(b)) != 0 && readCount != -1) {
            totalRead += readCount;
            fos.write(b, 0, readCount);
        }
        is.close();
        fos.close();
	}
	
}
