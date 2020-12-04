/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.item.json;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.json.domain.Trade;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Mahmoud Ben Hassine
 */
public abstract class JsonFileItemWriterFunctionalTests {

	private static final String EXPECTED_FILE_DIRECTORY = "src/test/resources/org/springframework/batch/item/json/";

	private Resource resource;
	private List<Trade> items;
	private ExecutionContext executionContext;
	private Trade trade1 = new Trade("123", 5, new BigDecimal("10.5"), "foo");
	private Trade trade2 = new Trade("456", 10, new BigDecimal("20.5"), "bar");
	private Trade trade3 = new Trade("789", 15, new BigDecimal("30.5"), "foobar");
	private Trade trade4 = new Trade("987", 20, new BigDecimal("40.5"), "barfoo");

	protected abstract JsonObjectMarshaller<Trade> getJsonObjectMarshaller();
	protected abstract JsonObjectMarshaller<Trade> getJsonObjectMarshallerWithPrettyPrint();
	protected abstract String getExpectedPrettyPrintedFile();

	private JsonFileItemWriter<Trade> writer;

	@Before
	public void setUp() throws Exception {
		Path outputFilePath = Paths.get("build", "trades.json");
		Files.deleteIfExists(outputFilePath);
		this.resource = new FileSystemResource(outputFilePath.toFile());
		this.executionContext = new ExecutionContext();
		this.items = Arrays.asList(this.trade1, this.trade2);
		this.writer = new JsonFileItemWriterBuilder<Trade>()
				.name("tradesItemWriter")
				.resource(this.resource)
				.jsonObjectMarshaller(getJsonObjectMarshaller())
				.build();
	}

	@Test
	public void testJsonWriting() throws Exception {
		// when
		this.writer.open(this.executionContext);
		this.writer.write(this.items);
		this.writer.close();

		// then
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "expected-trades.json"),
				this.resource.getFile());
	}

	@Test
	public void testJsonWritingWithMultipleWrite() throws Exception {
		// when
		this.writer.open(this.executionContext);
		this.writer.write(this.items);
		this.writer.write(Arrays.asList(trade3, trade4));
		this.writer.close();

		// then
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "expected-trades-with-multiple-writes.json"),
				this.resource.getFile());
	}

	@Test
	public void testJsonWritingWithPrettyPrinting() throws Exception {
		// given
		this.writer = new JsonFileItemWriterBuilder<Trade>()
				.name("tradesItemWriter")
				.resource(this.resource)
				.jsonObjectMarshaller(getJsonObjectMarshallerWithPrettyPrint())
				.build();

		// when
		this.writer.open(this.executionContext);
		this.writer.write(this.items);
		this.writer.close();

		// when
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + getExpectedPrettyPrintedFile()),
				this.resource.getFile());
	}

	@Test
	public void testJsonWritingWithEnclosingObject() throws Exception {
		// given
		this.writer.setHeaderCallback(writer -> writer.write("{\"trades\":["));
		this.writer.setFooterCallback(writer -> writer.write(JsonFileItemWriter.DEFAULT_LINE_SEPARATOR + "]}"));

		// when
		this.writer.open(this.executionContext);
		this.writer.write(this.items);
		this.writer.close();

		// then
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "expected-trades-with-wrapper-object.json"),
				this.resource.getFile());
	}

	@Test
	public void testForcedWrite() throws Exception {
		// given
		this.writer.setForceSync(true);

		// when
		this.writer.open(this.executionContext);
		this.writer.write(Collections.singletonList(this.trade1));
		this.writer.close();

		// then
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "expected-trades1.json"),
				this.resource.getFile());
	}

	@Test
	public void testWriteWithDelete() throws Exception {
		// given
		this.writer.setShouldDeleteIfExists(true);

		// when
		this.writer.open(this.executionContext);
		this.writer.write(Collections.singletonList(this.trade1));
		this.writer.close();
		this.writer.open(this.executionContext);
		this.writer.write(Collections.singletonList(this.trade2));
		this.writer.close();

		// then
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "expected-trades2.json"),
				this.resource.getFile());
	}

	@Test
	public void testRestart() throws Exception {
		this.writer.open(this.executionContext);
		// write some lines
		this.writer.write(Collections.singletonList(this.trade1));
		// get restart data
		this.writer.update(this.executionContext);
		// close template
		this.writer.close();

		// init with correct data
		this.writer.open(this.executionContext);
		// write more lines
		this.writer.write(Collections.singletonList(this.trade2));
		// get statistics
		this.writer.update(this.executionContext);
		// close template
		this.writer.close();

		// verify what was written to the file
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY+ "expected-trades.json"),
				this.resource.getFile());

		// 2 lines were written to the file in total
		assertEquals(2, this.executionContext.getLong("tradesItemWriter.written"));
	}

	@Test
	public void testTransactionalRestart() throws Exception {
		this.writer.open(this.executionContext);
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write some lines
				this.writer.write(Collections.singletonList(this.trade1));
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			this.writer.update(this.executionContext);
			return null;
		});
		// close template
		this.writer.close();

		// init with correct data
		this.writer.open(this.executionContext);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write more lines
				this.writer.write(Collections.singletonList(this.trade2));
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			this.writer.update(this.executionContext);
			return null;
		});
		// close template
		this.writer.close();

		// verify what was written to the file
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY+ "expected-trades.json"),
				this.resource.getFile());

		// 2 lines were written to the file in total
		assertEquals(2, this.executionContext.getLong("tradesItemWriter.written"));
	}

	@Test
	public void testItemMarshallingFailure() throws Exception {
		this.writer.setJsonObjectMarshaller(item -> {
			throw new IllegalArgumentException("Bad item");
		});
		this.writer.open(this.executionContext);
		try {
			this.writer.write(Collections.singletonList(this.trade1));
			fail();
		}
		catch (IllegalArgumentException iae) {
			assertEquals("Bad item", iae.getMessage());
		}
		finally {
			this.writer.close();
		}

		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "empty-trades.json"),
				this.resource.getFile());
	}

	@Test
	/*
	 * If append=true a new output file should still be created on the first run (not restart).
	 */
	public void testAppendToNotYetExistingFile() throws Exception {
		Resource toBeCreated = new FileSystemResource("build/FlatFileItemWriterTests.out");

		File outputFile = toBeCreated.getFile(); //enable easy content reading and auto-delete the file

		assertFalse("output file does not exist yet", toBeCreated.exists());
		this.writer.setResource(toBeCreated);
		this.writer.setAppendAllowed(true);
		this.writer.afterPropertiesSet();

		this.writer.open(this.executionContext);
		assertTrue("output file was created", toBeCreated.exists());

		this.writer.write(Collections.singletonList(this.trade1));
		this.writer.close();
		assertFileEquals(
				new File(EXPECTED_FILE_DIRECTORY + "expected-trades1.json"),
				outputFile);
		outputFile.delete();
	}

	private void assertFileEquals(File expected, File actual) throws Exception {
		String expectedHash = DigestUtils.md5DigestAsHex(new FileInputStream(expected));
		String actualHash = DigestUtils.md5DigestAsHex(new FileInputStream(actual));
		Assert.assertEquals(expectedHash, actualHash);
	}
}
