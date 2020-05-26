/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.item.file;

import java.io.Writer;
import java.io.IOException;

/**
 * Callback interface for writing a header to a file.
 * 
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public interface FlatFileHeaderCallback {

	/**
	 * Write contents to a file using the supplied {@link Writer}. It is not
	 * required to flush the writer inside this method.
	 *
	 * @param writer the {@link Writer} to be used to write the header.
	 *
	 * @throws IOException if error occurs during writing.
	 */
	void writeHeader(Writer writer) throws IOException;
}
