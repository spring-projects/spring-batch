package org.springframework.batch.item.util;

import java.io.File;
import java.io.IOException;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.util.Assert;

/**
 * Utility methods for files used in batch processing.
 * 
 * @author Peter Zozom
 */
public class FileUtils {

	// forbids instantiation
	private FileUtils() {
	}

	/**
	 * Set up output file for batch processing. This method implements common
	 * logic for handling output files when starting or restarting file I/O.
	 * When starting output file processing, creates/overwrites new file. When
	 * restarting output file processing, checks whether file is writable.
	 * 
	 * @param file file to be set up
	 * @param restarted true signals that we are restarting output file
	 * processing
	 * @param overwriteOutputFile If set to true, output file will be
	 * overwritten (this flag is ignored when processing is restart)
	 * 
	 * @throws IllegalArgumentException when file is null
	 * @throws ItemStreamException when starting output file processing, file
	 * exists and flag "overwriteOutputFile" is set to false
	 * @throws ItemStreamException when unable to create file or file is not
	 * writable
	 */
	public static void setUpOutputFile(File file, boolean restarted, boolean overwriteOutputFile) {

		Assert.notNull(file);

		try {
			if (!restarted) {
				if (file.exists()) {
					if (!overwriteOutputFile) {
						throw new ItemStreamException("File already exists: [" + file.getAbsolutePath() + "]");
					}
					file.delete();
				}

				if (file.getParent() != null) {
					new File(file.getParent()).mkdirs();
				}
				file.createNewFile();
				if (!file.exists()) {
					throw new ItemStreamException("Output file was not created: [" + file.getAbsolutePath()
								+ "]");
				}
			}
		}
		catch (IOException ioe) {
			throw new ItemStreamException("Unable to create file: [" + file.getAbsolutePath() + "]", ioe);
		}

		if (!file.canWrite()) {
			throw new ItemStreamException("File is not writable: [" + file.getAbsolutePath() + "]");
		}
	}
}
