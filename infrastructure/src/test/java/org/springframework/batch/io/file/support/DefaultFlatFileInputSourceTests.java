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

package org.springframework.batch.io.file.support;

import java.io.IOException;

import junit.framework.TestCase;

import org.springframework.batch.io.file.FieldSet;
import org.springframework.batch.io.file.support.DefaultFlatFileInputSource;
import org.springframework.batch.io.file.support.transform.LineTokenizer;
import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Tests for {@link DefaultFlatFileInputSource}
 * 
 * @author robert.kasanicky
 * 
 * TODO only regular reading is tested currently, add exception cases, restart,
 * skip, validation...
 */
public class DefaultFlatFileInputSourceTests extends TestCase {

	// object under test
	private DefaultFlatFileInputSource template = new DefaultFlatFileInputSource();

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	// simple stub instead of a realistic tokenizer
	private LineTokenizer tokenizer = new LineTokenizer() {
		public FieldSet tokenize(String line) {
			return new FieldSet(new String[]{line});
		}
	};

	/**
	 * Create inputFile, inject mock/stub dependencies for tested object,
	 * initialize the tested object
	 */
	protected void setUp() throws Exception {

		template.setResource(getInputResource(TEST_STRING));
		template.setTokenizer(tokenizer);

		// context argument is necessary only for the FileLocator, which
		// is mocked
		template.open();
	}

	/**
	 * Release resources and delete the temporary file
	 */
	protected void tearDown() throws Exception {
		template.close();
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	/**
	 * Test skip and skipRollback functionality
	 * @throws IOException
	 */
	public void testSkip() throws IOException {

		template.close();
		template.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		template.open();

		// read some records
		template.readFieldSet(); // #1
		template.readFieldSet(); // #2
		// commit them
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// read next record
		template.readFieldSet(); // # 3
		// mark record as skipped
		template.skip();
		// read next records
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		// we should now process all records after first commit point, that are
		// not marked as skipped
		assertEquals("[testLine4]", template.readFieldSet().toString());

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
	public void testTransactionSynchronizationUnknown() throws IOException {

		template.close();
		template.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		template.open();

		// read some records
		template.readFieldSet();
		template.skip();
		template.readFieldSet();
		// TODO
		// statistics = template.getStatistics();
		// skipped = (String)
		// statistics.get(FlatFileInputTemplate.SKIPPED_STATISTICS_NAME);
		// read = (String)
		// statistics.get(FlatFileInputTemplate.READ_STATISTICS_NAME);

		// call unknown, which has no influence and therefore statistics should
		// be the same
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		// TODO
		// statistics = template.getStatistics();
		// assertEquals(skipped, (String)
		// statistics.get(FlatFileInputTemplate.SKIPPED_STATISTICS_NAME));
		// assertEquals(read, (String)
		// statistics.get(FlatFileInputTemplate.READ_STATISTICS_NAME));
	}
	
	public void testRestartFromNullData() throws Exception {
		template.restoreFrom(null);
		assertEquals("[FlatFileInputTemplate-TestData]", template.readFieldSet().toString());
	}
	
	public void testRestartWithNullReader() throws Exception {
		template = new DefaultFlatFileInputSource();
		template.setResource(getInputResource(TEST_STRING));
		// do not open the template...
		template.restoreFrom(template.getRestartData());
		assertEquals("[FlatFileInputTemplate-TestData]", template.readFieldSet().toString());
	}

	public void testRestart() throws IOException {

		template.close();
		template.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		template.open();

		// read some records
		template.readFieldSet();
		template.readFieldSet();
		// commit them
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// read next two records
		template.readFieldSet();
		template.readFieldSet();

		// get restart data
		RestartData restartData = template.getRestartData();
		// TODO
		// assertEquals("4", (String) restartData);
		// close input
		template.close();

		template.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		template.open();
		template.restoreFrom(restartData);

		// read remaining records
		assertEquals("[testLine5]", template.readFieldSet().toString());
		assertEquals("[testLine6]", template.readFieldSet().toString());

		// TODO
		// Map statistics = template.getStatistics();
		// assertEquals("6",
		// statistics.get(FlatFileInputTemplate.READ_STATISTICS_NAME));
	}

}
