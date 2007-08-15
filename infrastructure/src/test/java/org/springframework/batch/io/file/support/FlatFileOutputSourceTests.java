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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.io.file.support.transform.Converter;
import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * Tests of regular usage for {@link FlatFileOutputSource} Exception cases will
 * be in separate TestCase classes with different <code>setUp</code> and
 * <code>tearDown</code> methods
 * 
 * @author robert.kasanicky
 * @author Dave Syer
 * 
 */
public class FlatFileOutputSourceTests extends TestCase {

	// object under test
	private FlatFileOutputSource template = new FlatFileOutputSource();

	// String to be written into file by the FlatFileInputTemplate
	private static final String TEST_STRING = "FlatFileOutputTemplateTest-OutputData";

	// temporary output file
	private File outputFile;

	// reads the output file to check the result
	private BufferedReader reader;

	/**
	 * Create temporary output file, define mock behaviour, set dependencies
	 * and initialize the object under test
	 */
	protected void setUp() throws Exception {
		
		outputFile = File.createTempFile("flatfile-output-", ".tmp");
		outputFile.createNewFile();

		template.setResource(new FileSystemResource(outputFile));
		template.afterPropertiesSet();

		template.open();

		reader = new BufferedReader(new FileReader(outputFile));
	}

	/**
	 * Release resources and delete the temporary output file
	 */
	protected void tearDown() throws Exception {
		reader.close();
		template.close();
		outputFile.delete();
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteString() throws IOException {
		template.write(TEST_STRING);

		String lineFromFile = reader.readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteCollection() throws IOException {
		template.write(Collections.singleton(TEST_STRING));

		String lineFromFile = reader.readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteWithConverter() throws IOException {
		template.setConverter(new Converter() {
			public Object convert(Object input) {
				return "FOO:" + input;
			}
		});
		Object data = new Object();
		template.write(data);

		String lineFromFile = reader.readLine();
		// converter not used if input is String
		assertEquals("FOO:" + data.toString(), lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteWithConverterAndInfiniteLoop() throws IOException {
		template.setConverter(new Converter() {
			public Object convert(Object input) {
				return "FOO:" + input;
			}
		});
		Object data = new Object();
		template.write(data);

		String lineFromFile = reader.readLine();
		// converter not used if input is String
		assertEquals("FOO:" + data.toString(), lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteWithConverterAndInfiniteLoopInCollection() throws IOException {
		template.setConverter(new Converter() {
			public Object convert(Object input) {
				return "FOO:" + input;
			}
		});
		Object data = new Object();
		template.write(new Object[] { data, data });

		String lineFromFile = reader.readLine();
		assertEquals("FOO:" + data.toString(), lineFromFile);
		lineFromFile = reader.readLine();
		assertEquals("FOO:" + data.toString(), lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteWithConverterAndInfiniteLoopInConvertedCollection() throws IOException {
		template.setConverter(new Converter() {
			boolean converted = false;
			public Object convert(Object input) {
				if (converted) {
					return input;
				}
				converted = true;
				return new Object[] { input, input };
			}
		});
		Object data = new Object();
		try {
			template.write(data);
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected
			assertTrue("Wrong message: "+e, e.getMessage().toLowerCase().indexOf("infinite")>=0);
		}

		String lineFromFile = reader.readLine();
		assertNull(lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteWithConverterAndString() throws IOException {
		template.setConverter(new Converter() {
			public Object convert(Object input) {
				return "FOO:" + input;
			}
		});
		template.write(Collections.singleton(TEST_STRING));

		String lineFromFile = reader.readLine();
		// converter not used if input is String
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteWithConverterAndCollectionOfString() throws IOException {
		template.setConverter(new Converter() {
			public Object convert(Object input) {
				return "FOO:" + input;
			}
		});
		template.write(TEST_STRING);

		String lineFromFile = reader.readLine();
		// converter not used if input is String
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	public void testWriteArray() throws IOException {
		template.write(new String[] { TEST_STRING, TEST_STRING });

		String lineFromFile = reader.readLine();
		assertEquals(TEST_STRING, lineFromFile);
		lineFromFile = reader.readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String[], LineDescriptor)</code> method
	 */
	public void testWriteRecord() throws IOException {
		String args = "1";

		// AggregatorStub ignores the LineDescriptor, so we pass null
		template.write(args);

		String lineFromFile = reader.readLine();
		assertEquals(args, lineFromFile);
	}

	public void testRollback() throws Exception {
		template.write("testLine1");
		// rollback
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		String lineFromFile = reader.readLine();
		assertEquals(null, lineFromFile);
	}

	public void testCommit() throws Exception {
		template.write("testLine1");
		// rollback
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		String lineFromFile = reader.readLine();
		assertEquals("testLine1", lineFromFile);
	}

	public void testUnknown() throws Exception {
		template.write("testLine1");
		// rollback
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
		String lineFromFile = reader.readLine();
		assertEquals("testLine1", lineFromFile);
	}

	public void testRestart() throws IOException {

		// write some lines
		template.write("testLine1");
		template.write("testLine2");
		template.write("testLine3");

		// commit
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

		// this will be rolled back...
		template.write("this will be rolled back");

		// rollback
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

		// write more lines
		template.write("testLine4");
		template.write("testLine5");

		// commit
		template.getTransactionSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

		// get restart data
		RestartData restartData = template.getRestartData();
		// close template
		template.close();

		// init for restart
		template.setBufferSize(0);
		template.open();

		// try empty restart data...
		try {
			template.restoreFrom(null);
			assertTrue(true);
		}
		catch (IllegalArgumentException iae) {
			fail("null restart data should be handled gracefully");
		}

		// init with correct data
		template.restoreFrom(restartData);

		// write more lines
		template.write("testLine6");
		template.write("testLine7");
		template.write("testLine8");

		// close template
		template.close();

		// verify what was written to the file
		for (int i = 1; i < 9; i++) {
			assertEquals("testLine" + i, reader.readLine());
		}

		// get statistics
		// Statistics statistics = template.getStatistics();
		// 3 lines were written to the file after restart
		// TODO
		// assertEquals("3",
		// statistics.get(FlatFileOutputTemplate.WRITTEN_STATISTICS_NAME));

	}

	public void testAfterPropertiesSetChecksMandatory() throws Exception {
		template = new FlatFileOutputSource();
		try {
			template.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testDefaultRestartData() throws Exception {
		template = new FlatFileOutputSource();
		RestartData restartData = template.getRestartData();
		assertNotNull(restartData);
		// TODO: assert the properties of the default restart data
		assertEquals(1, restartData.getProperties().size());
	}
}
