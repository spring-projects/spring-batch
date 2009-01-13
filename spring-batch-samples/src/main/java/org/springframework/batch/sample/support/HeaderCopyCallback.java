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

package org.springframework.batch.sample.support;

import java.io.IOException;
import java.io.Writer;

import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.LineCallbackHandler;
import org.springframework.util.Assert;

/**
 * Designed to be registered with both {@link FlatFileItemReader} and
 * {@link FlatFileItemWriter} and copy header line from input file to output
 * file.
 */
public class HeaderCopyCallback implements LineCallbackHandler, FlatFileHeaderCallback {

	private String header = "";
	
	public void handleLine(String line) {
		Assert.notNull(line);
		this.header = line;
	}

	public void writeHeader(Writer writer) throws IOException {
		writer.write("header from input: " + header);
		
	}

}
