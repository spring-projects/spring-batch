/*
 * Copyright 2008-2020 the original author or authors.
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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemCountAware;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.separator.RecordSeparatorPolicy;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link FlatFileItemReader}.
 */
public class FlatFileItemReaderTests {

	// common value used for writing to a file
	private String TEST_STRING = "FlatFileInputTemplate-TestData";

	private FlatFileItemReader<String> reader = new FlatFileItemReader<>();

	private FlatFileItemReader<Item> itemReader = new FlatFileItemReader<>();

	private ExecutionContext executionContext = new ExecutionContext();

	private Resource inputResource2 = getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6");

	private Resource inputResource1 = getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6");

	@Before
	public void setUp() {

		reader.setResource(inputResource1);
		reader.setLineMapper(new PassThroughLineMapper());

		itemReader.setResource(inputResource2);
		itemReader.setLineMapper(new ItemLineMapper());
	}

	@Test
	public void testRestartWithCustomRecordSeparatorPolicy() throws Exception {

		reader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
			// 1 record = 2 lines
			boolean pair = true;

			@Override
			public boolean isEndOfRecord(String line) {
				pair = !pair;
				return pair;
			}

			@Override
			public String postProcess(String record) {
				return record;
			}

			@Override
			public String preProcess(String record) {
				return record;
			}
		});

		reader.open(executionContext);

		assertEquals("testLine1testLine2", reader.read());
		assertEquals("testLine3testLine4", reader.read());

		reader.update(executionContext);

		reader.close();

		reader.open(executionContext);

		assertEquals("testLine5testLine6", reader.read());
	}

	@Test
	public void testCustomRecordSeparatorPolicyEndOfFile() throws Exception {

		reader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
			// 1 record = 2 lines
			boolean pair = true;

			@Override
			public boolean isEndOfRecord(String line) {
				pair = !pair;
				return pair;
			}

			@Override
			public String postProcess(String record) {
				return record;
			}

			@Override
			public String preProcess(String record) {
				return record;
			}
		});

		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\n"));
		reader.open(executionContext);

		assertEquals("testLine1testLine2", reader.read());

		try {
			reader.read();
			fail("Expected Exception");
		}
		catch (FlatFileParseException e) {
			// File ends in the middle of a record
			assertEquals(3, e.getLineNumber());
			assertEquals("testLine3", e.getInput());
		}

	}

	@Test
	public void testCustomRecordSeparatorBlankLine() throws Exception {

		reader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {

			@Override
			public boolean isEndOfRecord(String line) {
				return StringUtils.hasText(line);
			}

			@Override
			public String postProcess(String record) {
				return StringUtils.hasText(record) ? record : null;
			}

			@Override
			public String preProcess(String record) {
				return record;
			}
		});

		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\n\n"));
		reader.open(executionContext);

		assertEquals("testLine1", reader.read());
		assertEquals("testLine2", reader.read());
		assertEquals("testLine3", reader.read());
		assertEquals(null, reader.read());

	}

	@Test
	public void testCustomRecordSeparatorMultilineBlankLineAfterEnd() throws Exception {

		reader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {

			// 1 record = 2 lines
			boolean pair = true;

			@Override
			public boolean isEndOfRecord(String line) {
				if (StringUtils.hasText(line)) {
					pair = !pair;
				}
				return pair;
			}

			@Override
			public String postProcess(String record) {
				return StringUtils.hasText(record) ? record : null;
			}

			@Override
			public String preProcess(String record) {
				return record;
			}
		});

		reader.setResource(getInputResource("testLine1\ntestLine2\n\n"));
		reader.open(executionContext);

		assertEquals("testLine1testLine2", reader.read());
		assertEquals(null, reader.read());

	}

	@Test
	public void testCustomCommentDetectionLogic() throws Exception {
		reader = new FlatFileItemReader<String>() {
			@Override
			protected boolean isComment(String line) {
				return super.isComment(line) || line.endsWith("2");
			}
		};
		reader.setResource(getInputResource("#testLine1\ntestLine2\n//testLine3\ntestLine4\n"));
		reader.setComments(new String[] {"#", "//"});
		reader.setLineMapper(new PassThroughLineMapper());
		reader.open(executionContext);

		assertEquals("testLine4", reader.read());
		assertNull(reader.read());

		reader.close();
	}

	@Test
	public void testRestartWithSkippedLines() throws Exception {

		reader.setLinesToSkip(2);
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// get restart data
		reader.update(executionContext);
		// read next two records
		reader.read();
		reader.read();

		assertEquals(2, executionContext.getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
		// close input
		reader.close();

		reader.setResource(getInputResource("header\nignoreme\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		reader.open(executionContext);

		// read remaining records
		assertEquals("testLine3", reader.read());
		assertEquals("testLine4", reader.read());

		reader.update(executionContext);
		assertEquals(4, executionContext.getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
	}

	@Test
	public void testCurrentItemCount() throws Exception {

		reader.setCurrentItemCount(2);
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// get restart data
		reader.update(executionContext);

		assertEquals(4, executionContext.getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
		// close input
		reader.close();

	}

	@Test
	public void testMaxItemCount() throws Exception {

		reader.setMaxItemCount(2);
		reader.open(executionContext);

		// read some records
		reader.read();
		reader.read();
		// get restart data
		reader.update(executionContext);
		assertNull(reader.read());

		assertEquals(2, executionContext.getInt(ClassUtils.getShortName(FlatFileItemReader.class) + ".read.count"));
		// close input
		reader.close();

	}

	@Test
	public void testMaxItemCountFromContext() throws Exception {

		reader.setMaxItemCount(2);
		executionContext.putInt(reader.getClass().getSimpleName() + ".read.count.max", Integer.MAX_VALUE);
		reader.open(executionContext);
		// read some records
		reader.read();
		reader.read();
		assertNotNull(reader.read());
		// close input
		reader.close();

	}

	@Test
	public void testCurrentItemCountFromContext() throws Exception {

		reader.setCurrentItemCount(2);
		executionContext.putInt(reader.getClass().getSimpleName() + ".read.count", 3);
		reader.open(executionContext);
		// read some records
		assertEquals("testLine4", reader.read());
		// close input
		reader.close();

	}

	@Test
	public void testMaxAndCurrentItemCount() throws Exception {

		reader.setMaxItemCount(2);
		reader.setCurrentItemCount(2);
		reader.open(executionContext);
		// read some records
		assertNull(reader.read());
		// close input
		reader.close();

	}

	@Test
	public void testNonExistentResource() throws Exception {

		Resource resource = new NonExistentResource();

		reader.setResource(resource);

		// afterPropertiesSet should only throw an exception if the Resource is
		// null
		reader.afterPropertiesSet();

		reader.setStrict(false);
		reader.open(executionContext);
		assertNull(reader.read());
		reader.close();
	}

	@Test
	public void testOpenBadIOInput() throws Exception {

		reader.setResource(new AbstractResource() {
			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException();
			}

			@Override
			public boolean exists() {
				return true;
			}
		});

		try {
			reader.open(executionContext);
			fail();
		}
		catch (ItemStreamException ex) {
			// expected
		}

		// read() should then return a null
		assertNull(reader.read());
		reader.close();

	}

	@Test
	public void testDirectoryResource() throws Exception {

		FileSystemResource resource = new FileSystemResource("build/data");
		resource.getFile().mkdirs();
		assertTrue(resource.getFile().isDirectory());
		reader.setResource(resource);
		reader.afterPropertiesSet();

		reader.setStrict(false);
		reader.open(executionContext);
		assertNull(reader.read());

	}

	@Test
	public void testRuntimeFileCreation() throws Exception {

		Resource resource = new NonExistentResource();

		reader.setResource(resource);

		// afterPropertiesSet should only throw an exception if the Resource is
		// null
		reader.afterPropertiesSet();

		// replace the resource to simulate runtime resource creation
		reader.setResource(getInputResource(TEST_STRING));
		reader.open(executionContext);
		assertEquals(TEST_STRING, reader.read());
	}

	/**
	 * In strict mode, resource must exist at the time reader is opened.
	 */
	@Test(expected = ItemStreamException.class)
	public void testStrictness() throws Exception {

		Resource resource = new NonExistentResource();

		reader.setResource(resource);
		reader.setStrict(true);

		reader.afterPropertiesSet();

		reader.open(executionContext);
	}

	/**
	 * Exceptions from {@link LineMapper} are wrapped as {@link FlatFileParseException} containing contextual info about
	 * the problematic line and its line number.
	 */
	@Test
	public void testMappingExceptionWrapping() throws Exception {
		LineMapper<String> exceptionLineMapper = new LineMapper<String>() {
			@Override
			public String mapLine(String line, int lineNumber) throws Exception {
				if (lineNumber == 2) {
					throw new Exception("Couldn't map line 2");
				}
				return line;
			}
		};
		reader.setLineMapper(exceptionLineMapper);
		reader.afterPropertiesSet();

		reader.open(executionContext);
		assertNotNull(reader.read());

		try {
			reader.read();
			fail();
		}
		catch (FlatFileParseException expected) {
			assertEquals(2, expected.getLineNumber());
			assertEquals("testLine2", expected.getInput());
			assertEquals("Couldn't map line 2", expected.getCause().getMessage());
			assertThat(expected.getMessage(), startsWith("Parsing error at line: 2 in resource=["));
			assertThat(expected.getMessage(), endsWith("], input=[testLine2]"));
		}
	}

	@Test
	public void testItemCountAware() throws Exception {
		itemReader.open(executionContext);
		Item item1 = itemReader.read();
		assertEquals("testLine1", item1.getValue());
		assertEquals(1, item1.getItemCount());
		Item item2 = itemReader.read();
		assertEquals("testLine2", item2.getValue());
		assertEquals(2, item2.getItemCount());
		itemReader.update(executionContext);
		itemReader.close();

		itemReader.open(executionContext);
		Item item3 = itemReader.read();
		assertEquals("testLine3", item3.getValue());
		assertEquals(3, item3.getItemCount());
	}

	@Test
	public void testItemCountAwareMultiLine() throws Exception {
		itemReader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {

			// 1 record = 2 lines
			boolean pair = true;

			@Override
			public boolean isEndOfRecord(String line) {
				if (StringUtils.hasText(line)) {
					pair = !pair;
				}
				return pair;
			}

			@Override
			public String postProcess(String record) {
				return StringUtils.hasText(record) ? record : null;
			}

			@Override
			public String preProcess(String record) {
				return record;
			}
		});

		itemReader.open(executionContext);
		Item item1 = itemReader.read();
		assertEquals("testLine1testLine2", item1.getValue());
		assertEquals(1, item1.getItemCount());
		Item item2 = itemReader.read();
		assertEquals("testLine3testLine4", item2.getValue());
		assertEquals(2, item2.getItemCount());
		itemReader.update(executionContext);
		itemReader.close();

		itemReader.open(executionContext);
		Item item3 = itemReader.read();
		assertEquals("testLine5testLine6", item3.getValue());
		assertEquals(3, item3.getItemCount());
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	private static class NonExistentResource extends AbstractResource {

		public NonExistentResource() {
		}

		@Override
		public boolean exists() {
			return false;
		}

		@Override
		public String getDescription() {
			return "NonExistentResource";
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}
	}

	private static class Item implements ItemCountAware {

		private String value;

		private int itemCount;

		public Item(String value) {
			this.value = value;
		}

		@SuppressWarnings("unused")
		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public void setItemCount(int count) {
			this.itemCount = count;
		}

		public int getItemCount() {
			return itemCount;
		}

	}

	private static final class ItemLineMapper implements LineMapper<Item>  {

		@Override
		public Item mapLine(String line, int lineNumber) throws Exception {
			return new Item(line);
		}

	}
}
