/*
 * Copyright 2018-2023 the original author or authors.
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

package org.springframework.batch.infrastructure.item.json;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.UnexpectedInputException;
import org.springframework.batch.infrastructure.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.infrastructure.item.json.domain.Trade;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mahmoud Ben Hassine
 */
abstract class JsonFileItemWriterFunctionalTests {

	private static final String EXPECTED_FILE_DIRECTORY = "src/test/resources/org/springframework/batch/infrastructure/item/json/";

	private final Trade trade1 = new Trade("123", 5, new BigDecimal("10.5"), "foo");

	private final Trade trade2 = new Trade("456", 10, new BigDecimal("20.5"), "bar");

	private final Trade trade3 = new Trade("789", 15, new BigDecimal("30.5"), "foobar");

	private final Trade trade4 = new Trade("987", 20, new BigDecimal("40.5"), "barfoo");

	protected abstract JsonObjectMarshaller<Trade> getJsonObjectMarshaller();

	protected abstract JsonObjectMarshaller<Trade> getJsonObjectMarshallerWithPrettyPrint();

	protected abstract String getExpectedPrettyPrintedFile();

	protected abstract String getMarshallerName();

	@Test
	void testJsonWriting() throws Exception {
		// given
		Path outputFilePath = Paths.get("target", "trades-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.build();

		// when
		writer.open(new ExecutionContext());
		writer.write(Chunk.of(this.trade1, this.trade2));
		writer.close();

		// then
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades.json"), resource.getFile());
	}

	@Test
	void testJsonWritingWithMultipleWrite() throws Exception {
		// given
		Path outputFilePath = Paths.get("target", "testJsonWritingWithMultipleWrite-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.build();

		// when
		writer.open(new ExecutionContext());
		writer.write(Chunk.of(this.trade1, this.trade2));
		writer.write(Chunk.of(this.trade3, this.trade4));
		writer.close();

		// then
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades-with-multiple-writes.json"),
				resource.getFile());
	}

	@Test
	void testJsonWritingWithPrettyPrinting() throws Exception {
		// given
		Path outputFilePath = Paths.get("target", "testJsonWritingWithPrettyPrinting-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshallerWithPrettyPrint())
			.build();

		// when
		writer.open(new ExecutionContext());
		writer.write(Chunk.of(this.trade1, this.trade2));
		writer.close();

		// when
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + getExpectedPrettyPrintedFile()), resource.getFile());
	}

	@Test
	void testJsonWritingWithEnclosingObject() throws Exception {
		// given
		Path outputFilePath = Paths.get("target",
				"testJsonWritingWithEnclosingObject-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.headerCallback(headerWriter -> headerWriter.write("{\"trades\":["))
			.footerCallback(footerWriter -> footerWriter.write(JsonFileItemWriter.DEFAULT_LINE_SEPARATOR + "]}"))
			.build();

		// when
		writer.open(new ExecutionContext());
		writer.write(Chunk.of(this.trade1, this.trade2));
		writer.close();

		// then
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades-with-wrapper-object.json"),
				resource.getFile());
	}

	@Test
	void testForcedWrite() throws Exception {
		// given
		Path outputFilePath = Paths.get("target", "testForcedWrite-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.forceSync(true)
			.build();

		// when
		writer.open(new ExecutionContext());
		writer.write(Chunk.of(this.trade1));
		writer.close();

		// then
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades1.json"), resource.getFile());
	}

	@Test
	void testWriteWithDelete() throws Exception {
		// given
		ExecutionContext executionContext = new ExecutionContext();
		Path outputFilePath = Paths.get("target", "testWriteWithDelete-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.shouldDeleteIfExists(true)
			.build();

		// when
		writer.open(executionContext);
		writer.write(Chunk.of(this.trade1));
		writer.close();
		writer.open(executionContext);
		writer.write(Chunk.of(this.trade2));
		writer.close();

		// then
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades2.json"), resource.getFile());
	}

	@Test
	void testRestart() throws Exception {
		// given
		ExecutionContext executionContext = new ExecutionContext();
		Path outputFilePath = Paths.get("target", "testRestart-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.build();

		// when
		writer.open(executionContext);
		// write some lines
		writer.write(Chunk.of(this.trade1));
		// get restart data
		writer.update(executionContext);
		// close template
		writer.close();

		// init with correct data
		writer.open(executionContext);
		// write more lines
		writer.write(Chunk.of(this.trade2));
		// get statistics
		writer.update(executionContext);
		// close template
		writer.close();

		// verify what was written to the file
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades.json"), resource.getFile());

		// 2 lines were written to the file in total
		assertEquals(2, executionContext.getLong("tradesItemWriter.written"));
	}

	@Test
	void testTransactionalRestart() throws Exception {
		// given
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		ExecutionContext executionContext = new ExecutionContext();
		Path outputFilePath = Paths.get("target", "testTransactionalRestart-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.build();

		// when
		writer.open(executionContext);

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write some lines
				writer.write(Chunk.of(this.trade1));
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
				writer.write(Chunk.of(this.trade2));
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
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades.json"), resource.getFile());

		// 2 lines were written to the file in total
		assertEquals(2, executionContext.getLong("tradesItemWriter.written"));
	}

	@Test
	void testItemMarshallingFailure() throws Exception {
		// given
		ExecutionContext executionContext = new ExecutionContext();
		Path outputFilePath = Paths.get("target", "testItemMarshallingFailure-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(resource)
			.jsonObjectMarshaller(item -> {
				throw new IllegalArgumentException("Bad item");
			})
			.build();

		// when
		writer.open(executionContext);
		Exception exception = assertThrows(IllegalArgumentException.class, () -> writer.write(Chunk.of(this.trade1)));
		assertEquals("Bad item", exception.getMessage());

		writer.close();
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "empty-trades.json"), resource.getFile());
	}

	@Test
	/*
	 * If append=true a new output file should still be created on the first run (not
	 * restart).
	 */
	void testAppendToNotYetExistingFile() throws Exception {
		// given
		ExecutionContext executionContext = new ExecutionContext();
		Path outputFilePath = Paths.get("target", "testAppendToNotYetExistingFile-" + getMarshallerName() + ".json");
		FileSystemResource resource = new FileSystemResource(outputFilePath);
		Files.deleteIfExists(outputFilePath);
		JsonFileItemWriter<Trade> writer = new JsonFileItemWriterBuilder<Trade>().name("tradesItemWriter")
			.resource(new FileSystemResource(outputFilePath))
			.jsonObjectMarshaller(getJsonObjectMarshaller())
			.append(true)
			.build();

		// when
		writer.open(executionContext);
		writer.write(Chunk.of(this.trade1));
		writer.close();

		// then
		assertFileEquals(new File(EXPECTED_FILE_DIRECTORY + "expected-trades1.json"), resource.getFile());
	}

	private void assertFileEquals(File expected, File actual) throws Exception {
		JSONAssert.assertEquals(getContent(expected), getContent(actual), false);
	}

	private String getContent(File file) throws IOException {
		return Files.readString(file.toPath());
	}

}
