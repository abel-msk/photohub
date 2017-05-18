package home.abel.photohub.utils;

import home.abel.photohub.service.ExceptionFileIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.EmptyFileFilter;

public class FileUtils {
	final static Logger logger = LoggerFactory.getLogger(FileUtils.class);
	
	/*=============================================================================================
	 * 
	 *    Get non existed file name
	 *      
	 =============================================================================================*/

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
	
	/*=============================================================================================
	 * 
	 *    Delete file
	 *      
	 =============================================================================================*/
	/**
	 * Delete file
	 * @see  #fileDelete(File, boolean)
	 * @param fileName
	 * @param forceDelete
	 * @return
	 * @throws ExceptionFileIO
	 */
	public static boolean fileDelete ( String fileName, boolean forceDelete ) throws ExceptionFileIO {
		 return fileDelete(new File(fileName),forceDelete);
	}
	/**
	 * Delete file.  
	 * 
	 * @param theFile   - The file object we will delete
	 * @param forceDelete  -  if is true  delete file, even it is a directory. But empty directory.
	 * @return - true for file deleted  false steel not. ( Unknown reason )
	 * @throws ExceptionFileIO  Cannot delete by various reasons.
	 */
	public static boolean fileDelete(File theFile, boolean forceDelete) throws ExceptionFileIO {
		
		if (! theFile.exists())  {
			logger.warn("Cannot delete. Non existen file = " + theFile.getAbsolutePath());
			return true;
		}
		
		if (! theFile.canWrite() ) {
			String errMsg = new String("Cannot delete. Access deny, file  = " + theFile.getAbsolutePath());
			logger.error(errMsg);
			throw new ExceptionFileIO(errMsg);
		}
		
		if ((theFile.isDirectory()) && ( ! forceDelete)) {
			String errMsg = new String("Cannot delete. File is a directory  = " + theFile.getAbsolutePath());
			logger.error(errMsg);
			throw new ExceptionFileIO(errMsg);
		} 
		else if (theFile.isDirectory() && (theFile.list( EmptyFileFilter.NOT_EMPTY ).length > 0)) {
			String errMsg = new String("Cannot delete. Delete non empty directory  = " + theFile.getAbsolutePath());
			logger.error(errMsg);
			throw new ExceptionFileIO(errMsg);
		}
		return  theFile.delete();
	}
		
	/*=============================================================================================
	 * 
	 *    Copy file
	 *      
	 =============================================================================================*/
	/**
	 * Copy file and create all needed directory for destination file path
	 * @param fromFile
	 * @param toFile
	 * @throws IOException 
	 * @throws ExceptionFileIO 
	 * @throws Exception 
	 */
	public static void copyFile(String fromFile, String toFile) throws  ExceptionFileIO 
	{
		File fileTo = new File(toFile);
		File fileFrom = new File(fromFile);

		if (isAccessable(fileFrom)){
			try {
				org.apache.commons.io.FileUtils.forceMkdir(fileTo.getParentFile());
				org.apache.commons.io.FileUtils.copyFile(fileFrom,fileTo);
			} catch (IOException e) {
				throw new ExceptionFileIO("Cannot create dir or copy file="+fileTo.getAbsolutePath(),e );
			}
		}
		else  {
			throw new ExceptionFileIO("File is not accessable. File="+ fromFile );
		}
	}
	
	
	
	public static void copyFile(InputStream is, File outFile)  throws ExceptionFileIO {
		try {
			org.apache.commons.io.FileUtils.forceMkdir(outFile.getParentFile());
			org.apache.commons.io.FileUtils.copyInputStreamToFile(is, outFile);
		} catch (IOException e) {
			throw new ExceptionFileIO("Cannot create dir or copy file="+outFile.getAbsolutePath(),e );
		}
	}
	
	/*=============================================================================================
	 * 
	 *    Normalize path
	 *      
	 =============================================================================================*/
	public static String normalize (String inputPath) {
		if ( inputPath.startsWith("file://") ) {
			inputPath = inputPath.substring(7);
		}
		return FilenameUtils.normalize(inputPath);
	}

	/*=============================================================================================
	 * 
	 *    Check for  FileExist
	 *      
	 =============================================================================================*/
	public static boolean isAccessable(String path) {
		File theFile = new File(path);
		
		return isAccessable(theFile);
		
	}
	public static boolean isAccessable(File theFile) {
		return theFile.exists()  && theFile.canRead();
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
