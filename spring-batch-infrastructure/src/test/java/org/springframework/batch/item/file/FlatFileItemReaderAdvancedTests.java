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

import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.mapping.DefaultFieldSet;
import org.springframework.batch.item.file.mapping.FieldSet;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
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
	private FlatFileItemReader reader = new FlatFileItemReader();

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	private ExecutionContext executionContext;

	// simple stub instead of a realistic tokenizer
	private LineTokenizer tokenizer = new LineTokenizer() {
		public FieldSet tokenize(String line) {
			return new DefaultFieldSet(new String[] { line });
		}
	};

	private FieldSetMapper fieldSetMapper = new FieldSetMapper() {
		public Object mapLine(FieldSet fs, int lineNum) {
			return fs;
		}
	};

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

	/**
	 * Test rollback functionality
	 * 
	 * @throws IOException
	 */
	public void testReset() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open(executionContext);

		// read some records
		reader.read(); // #1
		reader.read(); // #2
		// commit them
		reader.mark();
		// read next record
		reader.read(); // # 3
		// read next records
		reader.reset();

		// we should now process all records after first commit point
		assertEquals("[testLine3]", reader.read().toString());

	}

	/**
	 * Test skip and skipRollback functionality
	 * 
	 * @throws IOException
	 */
	public void testFailOnFirstChunk() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open(executionContext);

		// read some records
		reader.read(); // #1
		reader.read(); // #2
		reader.read(); // #3
		// rollback
		reader.reset();
		// read next record
		reader.read(); // should be #1

		// we should now process all records after first commit point, that are
		// not marked as skipped
		assertEquals("[testLine2]", reader.read().toString());

	}

	public void testRestart() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// commit them
		reader.mark();
		// read next two records
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

	public void testRestartWithHeader() throws Exception {

		reader.close(null);
		reader.setResource(getInputResource("header\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.setFirstLineIsHeader(true);
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// commit them
		reader.mark();
		// read next two records
		reader.read();
		reader.read();

		// get restart data
		reader.update(executionContext);
		assertEquals(4, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
		// close input
		reader.close(executionContext);

		reader.setResource(getInputResource("header\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open(executionContext);

		// read remaining records
		assertEquals("[testLine5]", reader.read().toString());
		assertEquals("[testLine6]", reader.read().toString());

		reader.update(executionContext);
		assertEquals(6, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
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
		// commit them
		reader.mark();
		// read next two records
		reader.read();
		reader.read();

		// get restart data
		reader.update(executionContext);
		assertEquals(4, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
		// close input
		reader.close(executionContext);

		reader.setResource(getInputResource("header\nignoreme\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open(executionContext);

		// read remaining records
		assertEquals("[testLine5]", reader.read().toString());
		assertEquals("[testLine6]", reader.read().toString());

		reader.update(executionContext);
		assertEquals(6, executionContext.getLong(ClassUtils.getShortName(FlatFileItemReader.class)
		        + ".read.count"));
	}
}
