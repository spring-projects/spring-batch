/*
 * Copyright 2006-2023 the original author or authors.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of regular usage for {@link FlatFileItemWriter} Exception cases will be in
 * separate TestCase classes with different <code>setUp</code> and <code>tearDown</code>
 * methods
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class FlatFileItemWriterTests {

	// object under test
	private FlatFileItemWriter<String> writer;

	// String to be written into file by the FlatFileInputTemplate
	private static final String TEST_STRING = "FlatFileOutputTemplateTest-OutputData";

	// temporary output file
	private File outputFile;

	// reads the output file to check the result
	private BufferedReader reader;

	private ExecutionContext executionContext;

	/**
	 * Create temporary output file, define mock behaviour, set dependencies and
	 * initialize the object under test
	 */
	@BeforeEach
	void setUp() throws Exception {
		writer = new FlatFileItemWriter<>(new PassThroughLineAggregator<>());
		outputFile = File.createTempFile("flatfile-test-output-", ".tmp");

		writer.setResource(new FileSystemResource(outputFile));
		writer.setLineSeparator("\n");
		writer.afterPropertiesSet();
		writer.setSaveState(true);
		writer.setEncoding("UTF-8");
		executionContext = new ExecutionContext();
	}

	/**
	 * Release resources and delete the temporary output file
	 */
	@AfterEach
	void tearDown() throws Exception {
		if (reader != null) {
			reader.close();
		}
		writer.close();
		outputFile.delete();
	}

	/*
	 * Read a line from the output file, if the reader has not been created, recreate.
	 * This method is only necessary because running the tests in a UNIX environment locks
	 * the file if it's open for writing.
	 */
	private String readLine() throws IOException {
		return readLine("UTF-8");
	}

	/*
	 * Read a line from the output file, if the reader has not been created, recreate.
	 * This method is only necessary because running the tests in a UNIX environment locks
	 * the file if it's open for writing.
	 */
	private String readLine(String encoding) throws IOException {

		if (reader == null) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(outputFile), encoding));
		}

		return reader.readLine();
	}

	/*
	 * Properly close the output file reader.
	 */
	private void closeReader() throws IOException {

		if (reader != null) {
			reader.close();
			reader = null;
		}
	}

	@Test
	void testWriteWithMultipleOpen() throws Exception {
		writer.open(executionContext);
		writer.write(Chunk.of("test1"));
		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		assertEquals("test1", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	void testWriteWithDelete() throws Exception {
		writer.open(executionContext);
		writer.write(Chunk.of("test1"));
		writer.close();
		assertEquals("test1", readLine());
		closeReader();
		writer.setShouldDeleteIfExists(true);
		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		assertEquals("test2", readLine());
	}

	@Test
	void testWriteWithAppend() throws Exception {
		writer.setAppendAllowed(true);
		writer.open(executionContext);
		writer.write(Chunk.of("test1"));
		writer.close();
		assertEquals("test1", readLine());
		closeReader();
		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		assertEquals("test1", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	void testWriteWithAppendRestartOnSecondChunk() throws Exception {
		// This should be overridden via the writer#setAppendAllowed(true)
		writer.setShouldDeleteIfExists(true);
		writer.setAppendAllowed(true);
		writer.open(executionContext);
		writer.write(Chunk.of("test1"));
		writer.close();
		assertEquals("test1", readLine());
		closeReader();
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.update(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		assertEquals("test1", readLine());
		assertEquals(TEST_STRING, readLine());
		assertEquals(TEST_STRING, readLine());
		assertNull(readLine());
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		closeReader();
		assertEquals("test1", readLine());
		assertEquals(TEST_STRING, readLine());
		assertEquals(TEST_STRING, readLine());
		assertNull(readLine());
	}

	@Test
	void testOpenTwice() {
		// opening the writer twice should cause no issues
		writer.open(executionContext);
		writer.open(executionContext);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	@Test
	void testWriteString() throws Exception {
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	void testForcedWriteString() throws Exception {
		writer.setForceSync(true);
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	@Test
	void testWriteWithConverter() throws Exception {
		writer.setLineAggregator(item -> "FOO:" + item);
		String data = "string";
		writer.open(executionContext);
		writer.write(Chunk.of(data));
		String lineFromFile = readLine();
		// converter not used if input is String
		assertEquals("FOO:" + data, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String)</code> method
	 */
	@Test
	void testWriteWithConverterAndString() throws Exception {
		writer.setLineAggregator(item -> "FOO:" + item);
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		String lineFromFile = readLine();
		assertEquals("FOO:" + TEST_STRING, lineFromFile);
	}

	/**
	 * Regular usage of <code>write(String[], LineDescriptor)</code> method
	 */
	@Test
	void testWriteRecord() throws Exception {
		writer.open(executionContext);
		writer.write(Chunk.of("1"));
		String lineFromFile = readLine();
		assertEquals("1", lineFromFile);
	}

	@Test
	void testWriteRecordWithrecordSeparator() throws Exception {
		writer.setLineSeparator("|");
		writer.open(executionContext);
		writer.write(Chunk.of(new String[] { "1", "2" }));
		String lineFromFile = readLine();
		assertEquals("1|2|", lineFromFile);
	}

	@Test
	void testRestart() throws Exception {

		writer.setFooterCallback(writer -> writer.write("footer"));

		writer.open(executionContext);
		// write some lines
		writer.write(Chunk.of(new String[] { "testLine1", "testLine2", "testLine3" }));
		// write more lines
		writer.write(Chunk.of(new String[] { "testLine4", "testLine5" }));
		// get restart data
		writer.update(executionContext);
		// close template
		writer.close();

		// init with correct data
		writer.open(executionContext);
		// write more lines
		writer.write(Chunk.of(new String[] { "testLine6", "testLine7", "testLine8" }));
		// get statistics
		writer.update(executionContext);
		// close template
		writer.close();

		// verify what was written to the file
		for (int i = 1; i <= 8; i++) {
			assertEquals("testLine" + i, readLine());
		}

		assertEquals("footer", readLine());

		// 8 lines were written to the file in total
		assertEquals(8, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".written"));

	}

	@Test
	void testWriteStringTransactional() throws Exception {
		writeStringTransactionCheck(null);
		assertEquals(TEST_STRING, readLine());
	}

	@Test
	void testWriteStringNotTransactional() {
		writer.setTransactional(false);
		writeStringTransactionCheck(TEST_STRING);
	}

	private void writeStringTransactionCheck(String expectedInTransaction) {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write(Chunk.of(TEST_STRING));
				assertEquals(expectedInTransaction, readLine());
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}

			return null;
		});
		writer.close();
	}

	@Test
	void testTransactionalRestart() throws Exception {

		writer.setFooterCallback(writer -> writer.write("footer"));

		writer.open(executionContext);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write some lines
				writer.write(Chunk.of(new String[] { "testLine1", "testLine2", "testLine3" }));
				// write more lines
				writer.write(Chunk.of(new String[] { "testLine4", "testLine5" }));
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		// close template
		writer.close();

		// init with correct data
		writer.open(executionContext);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write more lines
				writer.write(Chunk.of(new String[] { "testLine6", "testLine7", "testLine8" }));
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		// close template
		writer.close();

		// verify what was written to the file
		for (int i = 1; i <= 8; i++) {
			assertEquals("testLine" + i, readLine());
		}

		assertEquals("footer", readLine());

		// 8 lines were written to the file in total
		assertEquals(8, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".written"));

	}

	@Test
	// BATCH-1959
	void testTransactionalRestartWithMultiByteCharacterUTF8() throws Exception {
		testTransactionalRestartWithMultiByteCharacter("UTF-8");
	}

	@Test
	// BATCH-1959
	void testTransactionalRestartWithMultiByteCharacterUTF16BE() throws Exception {
		testTransactionalRestartWithMultiByteCharacter("UTF-16BE");
	}

	private void testTransactionalRestartWithMultiByteCharacter(String encoding) throws Exception {
		writer.setEncoding(encoding);
		writer.setFooterCallback(writer -> writer.write("footer"));

		writer.open(executionContext);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write some lines
				writer.write(Chunk.of(new String[] { "téstLine1", "téstLine2", "téstLine3" }));
				// write more lines
				writer.write(Chunk.of(new String[] { "téstLine4", "téstLine5" }));
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		// close template
		writer.close();

		// init with correct data
		writer.open(executionContext);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write more lines
				writer.write(Chunk.of(new String[] { "téstLine6", "téstLine7", "téstLine8" }));
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		// close template
		writer.close();

		// verify what was written to the file
		for (int i = 1; i <= 8; i++) {
			assertEquals("téstLine" + i, readLine(encoding));
		}

		assertEquals("footer", readLine(encoding));

		// 8 lines were written to the file in total
		assertEquals(8, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".written"));
	}

	@Test
	void testOpenWithNonWritableFile() throws Exception {
		writer = new FlatFileItemWriter<>(new PassThroughLineAggregator<>());
		FileSystemResource file = new FileSystemResource("target/no-such-file.foo");
		writer.setResource(file);
		new File(file.getFile().getParent()).mkdirs();
		file.getFile().createNewFile();
		assertTrue(file.exists(), "Test file must exist: " + file);
		assertTrue(file.getFile().setReadOnly(), "Test file set to read-only: " + file);
		assertFalse(file.getFile().canWrite(), "Should be readonly file: " + file);
		writer.afterPropertiesSet();
		Exception exception = assertThrows(IllegalStateException.class, () -> writer.open(executionContext));
		String message = exception.getMessage();
		assertTrue(message.contains("writable"), "Message does not contain 'writable': " + message);
	}

	@Test
	void testDefaultStreamContext() throws Exception {
		writer = new FlatFileItemWriter<>(new PassThroughLineAggregator<>());
		writer.setResource(new FileSystemResource(outputFile));
		writer.afterPropertiesSet();
		writer.setSaveState(true);
		writer.open(executionContext);
		writer.update(executionContext);
		assertNotNull(executionContext);
		assertEquals(2, executionContext.entrySet().size());
		assertEquals(0, executionContext.getLong(ClassUtils.getShortName(FlatFileItemWriter.class) + ".current.count"));
	}

	@Test
	void testWriteStringWithBogusEncoding() throws Exception {
		writer.setTransactional(false);
		writer.setEncoding("BOGUS");
		// writer.setShouldDeleteIfEmpty(true);
		Exception exception = assertThrows(ItemStreamException.class, () -> writer.open(executionContext));
		assertTrue(exception.getCause() instanceof UnsupportedCharsetException);
		writer.close();
		// Try and write after the exception on open:
		writer.setEncoding("UTF-8");
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
	}

	@Test
	void testWriteStringWithEncodingAfterClose() throws Exception {
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		writer.setEncoding("UTF-8");
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		String lineFromFile = readLine();

		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	void testWriteFooter() throws Exception {
		writer.setFooterCallback(writer -> writer.write("a\nb"));
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		assertEquals(TEST_STRING, readLine());
		assertEquals("a", readLine());
		assertEquals("b", readLine());
	}

	@Test
	void testWriteHeader() throws Exception {
		writer.setHeaderCallback(writer -> writer.write("a\nb"));
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
	}

	@Test
	void testWriteWithAppendAfterHeaders() throws Exception {
		writer.setHeaderCallback(writer -> writer.write("a\nb"));
		writer.setAppendAllowed(true);
		writer.open(executionContext);
		writer.write(Chunk.of("test1"));
		writer.close();
		assertEquals("a", readLine());
		assertEquals("b", readLine());
		assertEquals("test1", readLine());
		closeReader();
		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		assertEquals("a", readLine());
		assertEquals("b", readLine());
		assertEquals("test1", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	void testWriteHeaderAndDeleteOnExit() {
		writer.setHeaderCallback(writer -> writer.write("a\nb"));
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		assertTrue(outputFile.exists());
		writer.close();
		assertFalse(outputFile.exists());
	}

	@Test
	void testDeleteOnExitReopen() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.update(executionContext);
		assertTrue(outputFile.exists());
		writer.close();
		assertFalse(outputFile.exists());
		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		assertEquals("test2", readLine());
	}

	@Test
	void testWriteHeaderAndDeleteOnExitReopen() throws Exception {
		writer.setHeaderCallback(writer -> writer.write("a\nb"));
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.update(executionContext);
		assertTrue(outputFile.exists());
		writer.close();
		assertFalse(outputFile.exists());

		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		assertEquals("a", readLine());
		assertEquals("b", readLine());
		assertEquals("test2", readLine());
	}

	@Test
	void testDeleteOnExitNoRecordsWrittenAfterRestart() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.write(Chunk.of("test2"));
		writer.update(executionContext);
		writer.close();
		assertTrue(outputFile.exists());
		writer.open(executionContext);
		writer.close();
		assertTrue(outputFile.exists());
	}

	@Test
	void testWriteHeaderAfterRestartOnFirstChunk() throws Exception {
		writer.setHeaderCallback(writer -> writer.write("a\nb"));
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
		lineFromFile = readLine();
		assertNull(lineFromFile);
	}

	@Test
	void testWriteHeaderAfterRestartOnSecondChunk() throws Exception {
		writer.setHeaderCallback(writer -> writer.write("a\nb"));
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.update(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		String lineFromFile = readLine();
		assertEquals("a", lineFromFile);
		lineFromFile = readLine();
		assertEquals("b", lineFromFile);
		lineFromFile = readLine();
		assertEquals(TEST_STRING, lineFromFile);
		writer.open(executionContext);
		writer.write(Chunk.of(TEST_STRING));
		writer.close();
		closeReader();
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
	void testLineAggregatorFailure() throws Exception {

		writer.setLineAggregator(item -> {
			if (item.equals("2")) {
				throw new RuntimeException("aggregation failed on " + item);
			}
			return item;
		});
		Chunk<String> items = Chunk.of("1", "2", "3");

		writer.open(executionContext);
		Exception expected = assertThrows(RuntimeException.class, () -> writer.write(items));
		assertEquals("aggregation failed on 2", expected.getMessage());

		// nothing was written to output
		assertNull(readLine());
	}

	@Test
	/*
	 * If append=true a new output file should still be created on the first run (not
	 * restart).
	 */
	void testAppendToNotYetExistingFile() throws Exception {
		WritableResource toBeCreated = new FileSystemResource("target/FlatFileItemWriterTests.out");

		outputFile = toBeCreated.getFile(); // enable easy content reading and auto-delete
											// the file

		assertFalse(toBeCreated.exists(), "output file does not exist yet");
		writer.setResource(toBeCreated);
		writer.setAppendAllowed(true);
		writer.afterPropertiesSet();

		writer.open(executionContext);
		assertTrue(toBeCreated.exists(), "output file was created");

		writer.write(Chunk.of("test1"));
		writer.close();
		assertEquals("test1", readLine());
	}

}
