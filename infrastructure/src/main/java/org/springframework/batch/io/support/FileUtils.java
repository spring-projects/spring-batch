package org.springframework.batch.io.support;

import java.io.File;
import java.io.IOException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.util.Assert;

/**
 * Utility methods for files used in batch processing.
 * 
 * @author peter.zozom
 */
public class FileUtils {

	/**
	 * Set up output file for batch processing. This method implements common logic for 
	 * handling output files when starting or restarting job/step.
	 * <p> When starting output file processing, method creates/overwrites new file. 
	 * When restaring output file processing, method checks whether file is writable. 
	 * 
	 * @param file file to be set up
	 * @param restarted TRUE signalizes that we are restarting output file processing
	 * @param overwriteOutputFile If set to TRUE, output file will be overwritten 
	 * (this flag is ignored when processing is restart)
	 * 
	 * @throws IllegalArgumentException when file is NULL
	 * @throws IllegalStateException when staring output file processing, file exists and 
	 * flag "shouldDeleteExisting" is set to FALSE 
	 * @throws DataAccessResourceFailureException when unable to create file or file is not writable
	 */
	public static void setUpOutputFile(File file, boolean restarted,
			boolean overwriteOutputFile) {

		Assert.notNull(file);

		try {
			if (!restarted) {
				if (file.exists()) {
					Assert.state(overwriteOutputFile, "File already exists: ["
							+ file.getAbsolutePath() + "]");
					file.delete();
				}

				if (file.getParent() != null ) {
					new File(file.getParent()).mkdirs();
				}
				file.createNewFile();
			}
		} catch (IOException ioe) {
			throw new DataAccessResourceFailureException(
					"Unable to create file: [" + file.getAbsolutePath() + "]",
					ioe);
		}

		if (!file.canWrite()) {
			throw new DataAccessResourceFailureException(
					"File is not writable: [" + file.getAbsolutePath() + "]");
		}
	}
}
