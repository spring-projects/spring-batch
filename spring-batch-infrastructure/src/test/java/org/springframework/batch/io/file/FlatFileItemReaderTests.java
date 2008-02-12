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

package org.springframework.batch.io.file;

import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.batch.io.file.mapping.DefaultFieldSet;
import org.springframework.batch.io.file.mapping.FieldSet;
import org.springframework.batch.io.file.mapping.FieldSetMapper;
import org.springframework.batch.io.file.transform.LineTokenizer;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link FlatFileItemReader} - skip and restart functionality.
 * 
 * @see FlatFileItemReaderBasicTests
 */
public class FlatFileItemReaderTests extends TestCase {

	// object under test
	private FlatFileItemReader reader = new FlatFileItemReader();

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	// simple stub instead of a realistic tokenizer
	private LineTokenizer tokenizer = new LineTokenizer() {
		public FieldSet tokenize(String line) {
			return new DefaultFieldSet(new String[] { line });
		}
	};

	private FieldSetMapper fieldSetMapper = new FieldSetMapper() {
		public Object mapLine(FieldSet fs) {
			return fs;
		}
	};

	/**
	 * Create inputFile, inject mock/stub dependencies for tested object,
	 * initialize the tested object
	 */
	protected void setUp() throws Exception {

		reader.setResource(getInputResource(TEST_STRING));
		reader.setLineTokenizer(tokenizer);
		reader.setFieldSetMapper(fieldSetMapper);
		// context argument is necessary only for the FileLocator, which
		// is mocked
		reader.open();
	}

	/**
	 * Release resources and delete the temporary file
	 */
	protected void tearDown() throws Exception {
		reader.close();
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	/**
	 * Test skip and skipRollback functionality
	 * @throws IOException
	 */
	public void testSkip() throws Exception {

		reader.close();
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open();

		// read some records
		reader.read(); // #1
		reader.read(); // #2
		// commit them
		reader.mark();
		// read next record
		reader.read(); // # 3
		// mark record as skipped
		reader.skip();
		// read next records
		reader.reset();

		// we should now process all records after first commit point, that are
		// not marked as skipped
		assertEquals("[testLine4]", reader.read().toString());

		// TODO update
		// Map statistics = template.getStatistics();
		// assertEquals("6",
		// statistics.get(FlatFileInputTemplate.READ_STATISTICS_NAME));
		// assertEquals("2",
		// statistics.get(FlatFileInputTemplate.SKIPPED_STATISTICS_NAME));

	}

	/**
	 * Test skip and skipRollback functionality
	 * @throws IOException
	 */
	public void testSkipFirstChunk() throws Exception {

		reader.close();
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open();

		// read some records
		reader.read(); // #1
		reader.read(); // #2
		reader.read(); // #3
		// mark record as skipped
		reader.skip();
		// rollback
		reader.reset();
		// read next record
		reader.read(); // should be #1

		// we should now process all records after first commit point, that are
		// not marked as skipped
		assertEquals("[testLine2]", reader.read().toString());

	}

	public void testRestartFromNullData() throws Exception {
		reader.restoreFrom(null);
		assertEquals("[FlatFileInputTemplate-TestData]", reader.read().toString());
	}

	public void testRestartBeforeOpen() throws Exception {
		reader = new FlatFileItemReader();
		reader.setResource(getInputResource(TEST_STRING));
		reader.setFieldSetMapper(fieldSetMapper);
		// do not open the template...
		try {
			reader.restoreFrom(reader.getExecutionAttributes());
		} catch (StreamException e) {
			assertTrue("Message does not contain open: "+e.getMessage(), e.getMessage().contains("open"));
		}
		assertEquals("[FlatFileInputTemplate-TestData]", reader.read().toString());
	}

	public void testRestart() throws Exception {

		reader.close();
		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.open();

		// read some records
		reader.read();
		reader.read();
		// commit them
		reader.mark();
		// read next two records
		reader.read();
		reader.read();

		// get restart data
		ExecutionAttributes streamContext = reader.getExecutionAttributes();
		assertEquals("4", (String) streamContext.getProperties().getProperty(
				FlatFileItemReader.READ_STATISTICS_NAME));
		// close input
		reader.close();

		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open();
		reader.restoreFrom(streamContext);

		// read remaining records
		assertEquals("[testLine5]", reader.read().toString());
		assertEquals("[testLine6]", reader.read().toString());

		ExecutionAttributes statistics = reader.getExecutionAttributes();
		assertEquals(6, statistics.getLong(FlatFileItemReader.READ_STATISTICS_NAME));
	}

}
