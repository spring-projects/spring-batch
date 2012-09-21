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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

/**
 * Tests of regular usage for {@link FlatFileItemWriter} Exception cases will be in separate TestCase classes with
 * different <code>setUp</code> and <code>tearDown</code> methods
 * 
 * @author Robert Kasanicky
 * @author Dave Syer
 * 
 */
public class FlatFileItemWriterTests {

	// object under test
	private FlatFileItemWriter<String> writer = new FlatFileItemWriter<String>();

	// String to be written into file by the FlatFileInputTemplate
	private static final String TEST_STRING = "FlatFileOutputTemplateTest-OutputData";

	// temporary output file
	private File outputFile;

	// reads the output file to check the result
	private BufferedReader reader;

	private ExecutionContext executionContext;

	/**
	 * Create temporary output file, define mock behaviour, set dependencies and initialize the object under test
	 */
	@Before
	public void setUp() throws Exception {

		outputFile = File.createTempFile("flatfile-test-output-", ".tmp");

		writer.setResource(new FileSystemResource(outputFile));
		writer.setLineSeparator("\n");
		writer.setLineAggregator(new PassThroughLineAggregator<String>());
		writer.afterPropertiesSet();
		writer.setSaveState(true);
		executionContext = new ExecutionContext();
	}

	/**
	 * Release resources and delete the temporary output file
	 */
	@After
	public void tearDown() throws Exception {
		if (reader != null) {
			reader.close();
		}
		writer.close();
		outputFile.delete();
	}

	/*
	 * Read a line from the output file, if the reader has not been created, recreate. This method is only necessary
	 * because running the tests in a UNIX environment locks the file if it's open for writing.
	 */
	private String readLine() throws IOException {

		if (reader == null) {
			reader = new BufferedReader(new FileReader(outputFile));
		}

		return reader.readLine();
	}

	@Test
	public void testWriteWithMultipleOpen() throws Exception {
		writer.open(executionContext);
		writer.write(Collections.singletonList("test1"));
		writer.open(executionContext);
		writer.write(Collections.singletonList("test2"));
		assertEquals("test1", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	public void testWriteWithDelete() throws Exception {
		writer.open(executionContext);
		writer.write(Collections.singletonList("test1"));
		writer.close();
		assertEquals("test1", readLine());
		reader = null;
		writer.setShouldDeleteIfExists(true);
		writer.open(executionContext);
		writer.write(Collections.singletonList("test2"));
		assertEquals("test2", readLine());
	}

	@Test
	public void testWriteWithAppend() throws Exception {
		writer.setAppendAllowed(true);
		writer.open(executionContext);
		writer.write(Collections.singletonList("test1"));
		writer.close();
		assertEquals("test1", readLine());
		reader = null;
		writer.open(executionContext);
		writer.write(Collections.singletonList("test2"));
		assertEquals("test1", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	public void testWriteWithAppendRestartOnSecondChunk() throws Exception {
		writer.setAppendAllowed(true);
		writer.open(executionContext);
		writer.write(Collections.singletonList("test1"));
		writer.close();
		assertEquals("test1", readLine());
		reader = null;
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.update(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		assertEquals("test1", readLine());
		assertEquals(TEST_STRING, readLine());
		assertEquals(TEST_STRING, readLine());
		assertEquals(null, readLine());
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		reader = null;
		assertEquals("test1", readLine());
		assertEquals(TEST_STRING, readLine());
		assertEquals(TEST_STRING, readLine());
		assertEquals(null, readLine());
	}

	@Test
	public void testOpenTwice() {
		// opening the writer twice should cause no issues
		writer.open(executionContext);
		writer.open(executionContext);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteString() throws Exception {
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	public void testForcedWriteString() throws Exception {
		writer.setForceSync(true);
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteWithConverter() throws Exception {
		writer.setLineAggregator(new LineAggregator<String>() {
			public String aggregate(String item) {
				return "FOO:" + item;
			}
		});
		String data = "string";
		writer.open(executionContext);
		writer.write(Collections.singletonList(data));
		String lineFromFile = readLine();
		// converter not used if input is String
		assertEquals("FOO:" + data, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteWithConverterAndString() throws Exception {
		writer.setLineAggregator(new LineAggregator<String>() {
			public String aggregate(String item) {
				return "FOO:" + item;
			}
		});
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		String lineFromFile = readLine();
		assertEquals("FOO:" + TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String[], LineDescriptor)</code> method
	 * 
	 * @throws Exception
	 */
	@Test
	public void testWriteRecord() throws Exception {
		writer.open(executionContext);
		writer.write(Collections.singletonList("1"));
		String lineFromFile = readLine();
		assertEquals("1", lineFromFile);
	}

	@Test
	public void testWriteRecordWithrecordSeparator() throws Exception {
		writer.setLineSeparator("|");
		writer.open(executionContext);
		writer.write(Arrays.asList(new String[] { "1", "2" }));
		String lineFromFile = readLine();
		assertEquals("1|2|", lineFromFile);
	}

	@Test
	public void testRestart() throws Exception {

		writer.setFooterCallback(new FlatFileFooterCallback() {

			public void writeFooter(Writer writer) throws IOException {
				writer.write("footer");
			}

		});

		writer.open(executionContext);
		// write some lines
		writer.write(Arrays.asList(new String[] { "testLine1", "testLine2", "testLine3" }));
		// write more lines
		writer.write(Arrays.asList(new String[] { "testLine4", "testLine5" }));
		// get restart data
		writer.update(executionContext);
		// close template
		writer.close();

		// init with correct data
		writer.open(executionContext);
		// write more lines
		writer.write(Arrays.asList(new String[] { "testLine6", "testLine7", "testLine8" }));
		// get statistics
		writer.update(executionContext);
		// close template
		writer.close();

		// verify what was written to the file
		for (int i = 1; i <= 8; i++) {
			assertEquals("testLine" + i, readLine());
		}

		assertEquals("footer", readLine());

		// 3 lines were written to the file after restart
		assertEquals(3, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".written"));

	}

	@Test
	public void testWriteStringTransactional() throws Exception {
		writeStringTransactionCheck(null);
		assertEquals(TEST_STRING, readLine());
	}

	@Test
	public void testWriteStringNotTransactional() throws Exception {
		writer.setTransactional(false);
		writeStringTransactionCheck(TEST_STRING);
	}

	private void writeStringTransactionCheck(final String expectedInTransaction) {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write(Collections.singletonList(TEST_STRING));
					assertEquals(expectedInTransaction, readLine());
				}
				catch (Exception e) {
					throw new UnexpectedInputException("Could not write data", e);
				}

				return null;
			}
		});
		writer.close();
	}

	@Test
	public void testTransactionalRestart() throws Exception {

		writer.setFooterCallback(new FlatFileFooterCallback() {

			public void writeFooter(Writer writer) throws IOException {
				writer.write("footer");
			}

		});

		writer.open(executionContext);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					// write some lines
					writer.write(Arrays.asList(new String[] { "testLine1", "testLine2", "testLine3" }));
					// write more lines
					writer.write(Arrays.asList(new String[] { "testLine4", "testLine5" }));
				}
				catch (Exception e) {
					throw new UnexpectedInputException("Could not write data", e);
				}
				// get restart data
				writer.update(executionContext);
				return null;
			}
		});
		// close template
		writer.close();

		// init with correct data
		writer.open(executionContext);

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					// write more lines
					writer.write(Arrays.asList(new String[] { "testLine6", "testLine7", "testLine8" }));
				}
				catch (Exception e) {
					throw new UnexpectedInputException("Could not write data", e);
				}
				// get restart data
				writer.update(executionContext);
				return null;
			}
		});
		// close template
		writer.close();

		// verify what was written to the file
		for (int i = 1; i <= 8; i++) {
			assertEquals("testLine" + i, readLine());
		}

		assertEquals("footer", readLine());

		// 3 lines were written to the file after restart
		assertEquals(3, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".written"));

	}

	@Test
	public void testOpenWithNonWritableFile() throws Exception {
		writer = new FlatFileItemWriter<String>();
		writer.setLineAggregator(new PassThroughLineAggregator<String>());
		FileSystemResource file = new FileSystemResource("target/no-such-file.foo");
		writer.setResource(file);
		new File(file.getFile().getParent()).mkdirs();
		file.getFile().createNewFile();
		assertTrue("Test file must exist: " + file, file.exists());
		assertTrue("Test file set to read-only: " + file, file.getFile().setReadOnly());
		assertFalse("Should be readonly file: " + file, file.getFile().canWrite());
		writer.afterPropertiesSet();
		try {
			writer.open(executionContext);
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			String message = e.getMessage();
			assertTrue("Message does not contain 'writable': " + message, message.indexOf("writable") >= 0);
		}
	}

	@Test
	public void testAfterPropertiesSetChecksMandatory() throws Exception {
		writer = new FlatFileItemWriter<String>();
		try {
			writer.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testDefaultStreamContext() throws Exception {
		writer = new FlatFileItemWriter<String>();
		writer.setResource(new FileSystemResource(outputFile));
		writer.setLineAggregator(new PassThroughLineAggregator<String>());
		writer.afterPropertiesSet();
		writer.setSaveState(true);
		writer.open(executionContext);
		writer.update(executionContext);
		assertNotNull(executionContext);
		assertEquals(2, executionContext.entrySet().size());
		assertEquals(0, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".current.count"));
	}

	@Test
	public void testWriteStringWithBogusEncoding() throws Exception {
		writer.setTransactional(false);
		writer.setEncoding("BOGUS");
		// writer.setShouldDeleteIfEmpty(true);
		try {
			writer.open(executionContext);
			fail("Expected ItemStreamException");
		}
		catch (ItemStreamException e) {
			assertTrue(e.getCause() instanceof UnsupportedCharsetException);
		}
		writer.close();
		// Try and write after the exception on open:
		writer.setEncoding("UTF-8");
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
	}

	@Test
	public void testWriteStringWithEncodingAfterClose() throws Exception {
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		writer.setEncoding("UTF-8");
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	public void testWriteFooter() throws Exception {
		writer.setFooterCallback(new FlatFileFooterCallback() {

			public void writeFooter(Writer writer) throws IOException {
				writer.write("a\nb");
			}

		});
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		assertEquals(TEST_STRING, readLine());
		assertEquals("a", readLine());
		assertEquals("b", readLine());
	}

	@Test
	public void testWriteHeader() throws Exception {
		writer.setHeaderCallback(new FlatFileHeaderCallback() {

			public void writeHeader(Writer writer) throws IOException {
				writer.write("a\nb");
			}

		});
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	public void testWriteWithAppendAfterHeaders() throws Exception {
		writer.setHeaderCallback(new FlatFileHeaderCallback() {
			public void writeHeader(Writer writer) throws IOException {
				writer.write("a\nb");
			}

		});
		writer.setAppendAllowed(true);
		writer.open(executionContext);
		writer.write(Collections.singletonList("test1"));
		writer.close();
		assertEquals("a", readLine());
		assertEquals("b", readLine());
		assertEquals("test1", readLine());
		reader = null;
		writer.open(executionContext);
		writer.write(Collections.singletonList("test2"));
		assertEquals("a", readLine());
		assertEquals("b", readLine());
		assertEquals("test1", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	public void testWriteHeaderAndDeleteOnExit() throws Exception {
		writer.setHeaderCallback(new FlatFileHeaderCallback() {

			public void writeHeader(Writer writer) throws IOException {
				writer.write("a\nb");
			}

		});
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		assertTrue(outputFile.exists());
		writer.close();
		assertFalse(outputFile.exists());
	}

	@Test
	public void testDeleteOnExitReopen() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		assertTrue(outputFile.exists());
		writer.close();
		assertFalse(outputFile.exists());
		writer.open(executionContext);
		writer.write(Collections.singletonList("test2"));
		assertEquals("test2", readLine());
	}

	@Test
	public void testWriteHeaderAfterRestartOnFirstChunk() throws Exception {
		writer.setHeaderCallback(new FlatFileHeaderCallback() {

			public void writeHeader(Writer writer) throws IOException {
				writer.write("a\nb");
			}

		});
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
		lineFromFile = readLine();
		assertEquals(null, lineFromFile);
	}

	@Test
	public void testWriteHeaderAfterRestartOnSecondChunk() throws Exception {
		writer.setHeaderCallback(new FlatFileHeaderCallback() {

			public void writeHeader(Writer writer) throws IOException {
				writer.write("a\nb");
			}

		});
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.update(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
		writer.open(executionContext);
		writer.write(Collections.singletonList(TEST_STRING));
		writer.close();
		reader = null;
		lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	/*
	 * Nothing gets written to file if line aggregation fails.
	 */
	public void testLineAggregatorFailure() throws Exception {

		writer.setLineAggregator(new LineAggregator<String>() {

			public String aggregate(String item) {
				if (item.equals("2")) {
					throw new RuntimeException("aggregation failed on " + item);
				}
				return item;
			}
		});
		List<String> items = new ArrayList<String>() {
			{
				add("1");
				add("2");
				add("3");
			}
		};

		writer.open(executionContext);
		try {
			writer.write(items);
			fail();
		}
		catch (RuntimeException expected) {
			assertEquals("aggregation failed on 2", expected.getMessage());
		}

		// nothing was written to output
		assertNull(readLine());
	}

	@Test
	/**
	 * If append=true a new output file should still be created on the first run (not restart).
	 */
	public void testAppendToNotYetExistingFile() throws Exception {
		Resource toBeCreated = new FileSystemResource("target/FlatFileItemWriterTests.out");
		
		outputFile = toBeCreated.getFile(); //enable easy content reading and auto-delete the file 
		
		assertFalse("output file does not exist yet", toBeCreated.exists());
		writer.setResource(toBeCreated);
		writer.setAppendAllowed(true);
		writer.afterPropertiesSet();

		writer.open(executionContext);
		assertTrue("output file was created", toBeCreated.exists());

		writer.write(Collections.singletonList("test1"));
		writer.close();
		assertEquals("test1", readLine());
	}
}
