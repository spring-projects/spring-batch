/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.file;

import java.io.File;
import java.io.Writer;
import java.io.IOException;
import org.springframework.batch.item.file.FlatFileHeaderCallback;

/**
 * Callback interface for writing to a header to a file after the file has been written
 * 
 * @author Juzer Ali
 */
public interface FlatFilePostWriteHeaderCallback {

	/**
	 * Write contents to a file using the supplied {@link Writer} after all the data
	 * has been written on the disk. Uses file merge. It is not
	 * required to flush the writer inside this method.
	 * @param file handle to file which has been written
	 * @param lineCount Number of lines in the file
	 */
	void writeHeader(Writer writer, File file, long lineCount) throws IOException;
}
