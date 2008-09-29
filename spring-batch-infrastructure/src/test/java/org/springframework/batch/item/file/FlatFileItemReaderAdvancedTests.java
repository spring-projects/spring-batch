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

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

/**
 * Tests for {@link FlatFileItemReader} - skip and restart functionality.
 * 
 * @see FlatFileItemReaderBasicTests
 */
public class FlatFileItemReaderAdvancedTests extends TestCase {

	// object under test
	private FlatFileItemReader<FieldSet> reader = new FlatFileItemReader<FieldSet>();

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	private ExecutionContext executionContext;

	// simple stub instead of a realistic tokenizer
	private LineTokenizer tokenizer = new LineTokenizer() {
		public FieldSet process(String line) {
			return new DefaultFieldSet(new String[] { line });
		}
	};

	private FieldSetMapper<FieldSet> fieldSetMapper = new PassThroughFieldSetMapper();

	/**
	 * Create inputFile, inject mock/stub dependencies for tested object, initialize the tested object
	 */
	protected void setUp() throws Exception {

		reader.setResource(getInputResource(TEST_STRING));
		reader.setLineTokenizer(tokenizer);
		reader.setFieldSetMapper(fieldSetMapper);
		reader.setSaveState(true);
		// context argument is necessary only for the FileLocator, which
		// is mocked
		executionContext = new ExecutionContext();
	}

	/**
	 * Release resources and delete the temporary file
	 */
	protected void tearDown() throws Exception {
		reader.close(null);
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	public void testRestart() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		reader.read();
		reader.read();

		// get restart data
		reader.update(executionContext);
		assertEquals(4, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
		// close input
		reader.close(executionContext);

		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open(executionContext);

		// read remaining records
		assertEquals("[testLine5]", reader.read().toString());
		assertEquals("[testLine6]", reader.read().toString());

		reader.update(executionContext);
		assertEquals(6, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
	}
	
	public void testRestartWithCustomRecordSeparatorPolicy() throws Exception {
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
			// 1 record = 2 lines
			boolean pair = true;

			public boolean isEndOfRecord(String line) {
				pair = !pair;
				return pair;
			}

			public String postProcess(String record) {
				return record;
			}

			public String preProcess(String record) {
				return record;
			}
		});

		reader.open(executionContext);

		assertEquals("[testLine1testLine2]", reader.read().toString());
		assertEquals("[testLine3testLine4]", reader.read().toString());
		
		reader.update(executionContext);
		
		reader.close(executionContext);
	
		reader.open(executionContext);
		
		assertEquals("[testLine5testLine6]", reader.read().toString());
	}

	public void testRestartWithHeader() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("header\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.setFirstLineIsHeader(true);
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// get restart data
		reader.update(executionContext);
		// read next two records
		reader.read();
		reader.read();

		assertEquals(2, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
		// close input
		reader.close(executionContext);

		reader.setResource(getInputResource("header\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open(executionContext);

		// read remaining records
		assertEquals("[testLine3]", reader.read().toString());
		assertEquals("[testLine4]", reader.read().toString());

		reader.update(executionContext);
		assertEquals(4, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
	}

	public void testRestartWithSkippedLines() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("header\nignoreme\n\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.setLinesToSkip(2);
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// get restart data
		reader.update(executionContext);
		// read next two records
		reader.read();
		reader.read();

		assertEquals(2, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
		// close input
		reader.close(executionContext);

		reader.setResource(getInputResource("header\nignoreme\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open(executionContext);

		// read remaining records
		assertEquals("[testLine3]", reader.read().toString());
		assertEquals("[testLine4]", reader.read().toString());

		reader.update(executionContext);
		assertEquals(4, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
	}
}
