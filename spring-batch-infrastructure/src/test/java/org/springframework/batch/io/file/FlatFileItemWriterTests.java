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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;

import junit.framework.TestCase;

import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.writer.ItemTransformer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Tests of regular usage for {@link FlatFileItemWriter} Exception cases will be
 * in separate TestCase classes with different <code>setUp</code> and
 * <code>tearDown</code> methods
 * 
 * @author robert.kasanicky
 * @author Dave Syer
 * 
 */
public class FlatFileItemWriterTests extends TestCase {

	// object under test
	private FlatFileItemWriter inputSource = new FlatFileItemWriter();

	// String to be written into file by the FlatFileInputTemplate
	private static final String TEST_STRING = "FlatFileOutputTemplateTest-OutputData";

	// temporary output file
	private File outputFile;

	// reads the output file to check the result
	private BufferedReader reader;

	/**
	 * Create temporary output file, define mock behaviour, set dependencies and
	 * initialize the object under test
	 */
	protected void setUp() throws Exception {

		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
		TransactionSynchronizationManager.initSynchronization();

		outputFile = File.createTempFile("flatfile-output-", ".tmp");

		inputSource.setResource(new FileSystemResource(outputFile));
		inputSource.afterPropertiesSet();

		inputSource.open();

	}

	/**
	 * Release resources and delete the temporary output file
	 */
	protected void tearDown() throws Exception {
		if (reader != null) {
			reader.close();
		}
		inputSource.close();
		outputFile.delete();
	}

	/*
	 * Read a line from the output file, if the reader has not been created,
	 * recreate. This method is only necessary because running the tests in a
	 * UNIX environment locks the file if it's open for writing.
	 */
	private String readLine() throws IOException {

		if (reader == null) {
			reader = new BufferedReader(new FileReader(outputFile));
		}

		return reader.readLine();
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * @throws Exception 
	 */
	public void testWriteString() throws Exception {
		inputSource.write(TEST_STRING);
		inputSource.close();
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * @throws Exception 
	 */
	public void testWriteCollection() throws Exception {
		inputSource.write(Collections.singleton(TEST_STRING));
		inputSource.close();
		String lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * @throws Exception 
	 */
	public void testWriteWithConverter() throws Exception {
		inputSource.setTransformer(new ItemTransformer() {
			public Object transform(Object input) {
				return "FOO:" + input;
			}
		});
		Object data = new Object();
		inputSource.write(data);
		inputSource.close();
		String lineFromFile = readLine();
		// converter not used if input is String
		assertEquals("FOO:" + data.toString(), lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * @throws Exception 
	 */
	public void testWriteWithConverterAndInfiniteLoop() throws Exception {
		inputSource.setTransformer(new ItemTransformer() {
			public Object transform(Object input) {
				return "FOO:" + input;
			}
		});
		Object data = new Object();
		inputSource.write(data);
		inputSource.close();
		String lineFromFile = readLine();
		// converter not used if input is String
		assertEquals("FOO:" + data.toString(), lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * @throws Exception 
	 */
	public void testWriteWithConverterAndString() throws Exception {
		inputSource.setTransformer(new ItemTransformer() {
			public Object transform(Object input) {
				return "FOO:" + input;
			}
		});
		inputSource.write(TEST_STRING);
		inputSource.close();
		String lineFromFile = readLine();
		assertEquals("FOO:"+TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * @throws Exception 
	 */
	public void testWriteArray() throws Exception {
		inputSource.write(new String[] { TEST_STRING, TEST_STRING });
		inputSource.close();
		String lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String[], LineDescriptor)</code> method
	 * @throws Exception 
	 */
	public void testWriteRecord() throws Exception {
		String args = "1";

		// AggregatorStub ignores the LineDescriptor, so we pass null
		inputSource.write(args);
		inputSource.close();
		String lineFromFile = readLine();
		assertEquals(args, lineFromFile);
	}

	public void testRollback() throws Exception {
		inputSource.write("testLine1");
		// rollback
		rollback();
		inputSource.close();
		String lineFromFile = readLine();
		assertEquals(null, lineFromFile);
	}

	public void testCommit() throws Exception {
		inputSource.write("testLine1");
		// rollback
		commit();
		inputSource.close();
		String lineFromFile = readLine();
		assertEquals("testLine1", lineFromFile);
	}

	public void testRestart() throws Exception {

		// write some lines
		inputSource.write("testLine1");
		inputSource.write("testLine2");
		inputSource.write("testLine3");

		// commit
		commit();

		// this will be rolled back...
		inputSource.write("this will be rolled back");

		// rollback
		rollback();

		// write more lines
		inputSource.write("testLine4");
		inputSource.write("testLine5");

		// commit
		commit();

		// get restart data
		ExecutionContext streamContext = inputSource.getExecutionContext();
		// close template
		inputSource.close();

		// init for restart
		inputSource.setBufferSize(0);
		inputSource.open();

		// try empty restart data...
		try {
			inputSource.restoreFrom(null);
			assertTrue(true);
		}
		catch (IllegalArgumentException iae) {
			fail("null restart data should be handled gracefully");
		}

		// init with correct data
		inputSource.restoreFrom(streamContext);

		// write more lines
		inputSource.write("testLine6");
		inputSource.write("testLine7");
		inputSource.write("testLine8");

		// get statistics
		ExecutionContext statistics = inputSource.getExecutionContext();
		// close template
		inputSource.close();

		// verify what was written to the file
		for (int i = 1; i < 9; i++) {
			assertEquals("testLine" + i, readLine());
		}

		// 3 lines were written to the file after restart
		assertEquals(3, statistics.getLong(FlatFileItemWriter.WRITTEN_STATISTICS_NAME));

	}

	public void testAfterPropertiesSetChecksMandatory() throws Exception {
		inputSource = new FlatFileItemWriter();
		try {
			inputSource.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	public void testDefaultStreamContext() throws Exception {
		inputSource = new FlatFileItemWriter();
		inputSource.open();
		ExecutionContext streamContext = inputSource.getExecutionContext();
		assertNotNull(streamContext);
		assertEquals(3, streamContext.getProperties().size());
		assertEquals(0, streamContext.getLong(FlatFileItemWriter.RESTART_DATA_NAME));
	}

	private void commit() {
		((ItemStream) inputSource).mark();
	}

	private void rollback() {
		((ItemStream) inputSource).reset();
	}

}
