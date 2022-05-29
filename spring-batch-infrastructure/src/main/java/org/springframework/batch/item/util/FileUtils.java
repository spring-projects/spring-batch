/*
 * Copyright 2006-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.util;

import java.io.File;
import java.io.IOException;

import org.springframework.batch.item.ItemStreamException;
import org.springframework.util.Assert;

/**
 * Utility methods for files used in batch processing.
 *
 * @author Peter Zozom
 * @author Mahmoud Ben Hassine
 */
public final class FileUtils {

	// forbids instantiation
	private FileUtils() {
	}

	/**
	 * Set up output file for batch processing. This method implements common logic for
	 * handling output files when starting or restarting file I/O. When starting output
	 * file processing, creates/overwrites new file. When restarting output file
	 * processing, checks whether file is writable.
	 * @param file file to be set up
	 * @param restarted true signals that we are restarting output file processing
	 * @param append true signals input file may already exist (but doesn't have to)
	 * @param overwriteOutputFile If set to true, output file will be overwritten (this
	 * flag is ignored when processing is restart)
	 */
	public static void setUpOutputFile(File file, boolean restarted, boolean append, boolean overwriteOutputFile) {

		Assert.notNull(file, "An output file is required");

		try {
			if (!restarted) {
				if (!append) {
					if (file.exists()) {
						if (!overwriteOutputFile) {
							throw new ItemStreamException("File already exists: [" + file.getAbsolutePath() + "]");
						}
						if (!file.delete()) {
							throw new IOException("Could not delete file: " + file);
						}
					}

					if (file.getParent() != null) {
						new File(file.getParent()).mkdirs();
					}
					if (!createNewFile(file)) {
						throw new ItemStreamException("Output file was not created: [" + file.getAbsolutePath() + "]");
					}
				}
				else {
					if (!file.exists()) {
						if (file.getParent() != null) {
							new File(file.getParent()).mkdirs();
						}
						if (!createNewFile(file)) {
							throw new ItemStreamException(
									"Output file was not created: [" + file.getAbsolutePath() + "]");
						}
					}
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

	/**
	 * Create a new file if it doesn't already exist.
	 * @param file the file to create on the filesystem
	 * @return true if file was created else false.
	 * @throws IOException is thrown if error occurs during creation and file does not
	 * exist.
	 */
	public static boolean createNewFile(File file) throws IOException {

		if (file.exists()) {
			return false;
		}

		try {
			return file.createNewFile() && file.exists();
		}
		catch (IOException e) {
			// On some file systems you can get an exception here even though the
			// files was successfully created
			if (file.exists()) {
				return true;
			}
			else {
				throw e;
			}
		}

	}

}
