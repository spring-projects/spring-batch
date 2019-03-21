/*
 * Copyright 2008-2016 the original author or authors.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.core.io.FileSystemResource;

/**
 * Tests for {@link MultiResourceItemWriter}.
 * 
 * @see MultiResourceItemWriterFlatFileTests
 * @see MultiResourceItemReaderXmlTests
 */
public class AbstractMultiResourceItemWriterTests {

	protected MultiResourceItemWriter<String> tested;

	protected File file;

	protected ResourceSuffixCreator suffixCreator = new SimpleResourceSuffixCreator();

	protected ExecutionContext executionContext = new ExecutionContext();

	protected void setUp(ResourceAwareItemWriterItemStream<String> delegate) throws Exception {
		tested = new MultiResourceItemWriter<>();
		tested.setResource(new FileSystemResource(file));
		tested.setDelegate(delegate);
		tested.setResourceSuffixCreator(suffixCreator);
		tested.setItemCountLimitPerResource(2);
		tested.setSaveState(true);
	}

	public void createFile() throws Exception {
		file = File.createTempFile(MultiResourceItemWriterFlatFileTests.class.getSimpleName(), null);
	}

	protected String readFile(File f) throws Exception {
		BufferedReader reader = new BufferedReader(new FileReader(f));
		StringBuilder result = new StringBuilder();
		try {
			while (true) {
				String line = reader.readLine();
				if (line == null) {
					break;
				}
				result.append(line);
			}
		}
		finally {
			reader.close();
		}
		return result.toString();
	}
}
