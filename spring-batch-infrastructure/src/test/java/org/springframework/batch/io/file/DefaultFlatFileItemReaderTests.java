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
import org.springframework.batch.item.StreamException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

/**
 * Tests for {@link DefaultFlatFileItemReader}
 * 
 * @author robert.kasanicky
 * 
 * TODO only regular reading is tested currently, add exception cases, restart,
 * skip, validation...
 */
public class DefaultFlatFileItemReaderTests extends TestCase {

	// object under test
	private DefaultFlatFileItemReader inputSource = new DefaultFlatFileItemReader();

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

		inputSource.setResource(getInputResource(TEST_STRING));
		inputSource.setTokenizer(tokenizer);
		inputSource.setFieldSetMapper(fieldSetMapper);
		// context argument is necessary only for the FileLocator, which
		// is mocked
		inputSource.open();
	}

	/**
	 * Release resources and delete the temporary file
	 */
	protected void tearDown() throws Exception {
		inputSource.close();
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	/**
	 * Test skip and skipRollback functionality
	 * @throws IOException
	 */
	public void testSkip() throws Exception {

		inputSource.close();
		inputSource.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		inputSource.open();

		// read some records
		inputSource.read(); // #1
		inputSource.read(); // #2
		// commit them
		inputSource.mark(null);
		// read next record
		inputSource.read(); // # 3
		// mark record as skipped
		inputSource.skip();
		// read next records
		inputSource.reset(null);

		// we should now process all records after first commit point, that are
		// not marked as skipped
		assertEquals("[testLine4]", inputSource.read().toString());

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

		inputSource.close();
		inputSource.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		inputSource.open();

		// read some records
		inputSource.read(); // #1
		inputSource.read(); // #2
		inputSource.read(); // #3
		// mark record as skipped
		inputSource.skip();
		// rollback
		inputSource.reset(null);
		// read next record
		inputSource.read(); // should be #1

		// we should now process all records after first commit point, that are
		// not marked as skipped
		assertEquals("[testLine2]", inputSource.read().toString());

	}

	public void testRestartFromNullData() throws Exception {
		inputSource.restoreFrom(null);
		assertEquals("[FlatFileInputTemplate-TestData]", inputSource.read().toString());
	}

	public void testRestartBeforeOpen() throws Exception {
		inputSource = new DefaultFlatFileItemReader();
		inputSource.setResource(getInputResource(TEST_STRING));
		inputSource.setFieldSetMapper(fieldSetMapper);
		// do not open the template...
		try {
			inputSource.restoreFrom(inputSource.getStreamContext());
		} catch (StreamException e) {
			assertTrue("Message does not contain open: "+e.getMessage(), e.getMessage().contains("open"));
		}
		assertEquals("[FlatFileInputTemplate-TestData]", inputSource.read().toString());
	}

	public void testRestart() throws Exception {

		inputSource.close();
		inputSource.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		inputSource.open();

		// read some records
		inputSource.read();
		inputSource.read();
		// commit them
		inputSource.mark(null);
		// read next two records
		inputSource.read();
		inputSource.read();

		// get restart data
		ExecutionAttributes streamContext = inputSource.getStreamContext();
		assertEquals("4", (String) streamContext.getProperties().getProperty(
				DefaultFlatFileItemReader.READ_STATISTICS_NAME));
		// close input
		inputSource.close();

		inputSource.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		inputSource.open();
		inputSource.restoreFrom(streamContext);

		// read remaining records
		assertEquals("[testLine5]", inputSource.read().toString());
		assertEquals("[testLine6]", inputSource.read().toString());

		ExecutionAttributes statistics = inputSource.getStreamContext();
		assertEquals(6, statistics.getLong(DefaultFlatFileItemReader.READ_STATISTICS_NAME));
	}

}
