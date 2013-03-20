package org.springframework.batch.item.file;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

	private FlatFileItemReader<String> reader = new FlatFileItemReader<String>();

	private FlatFileItemReader<Item> itemReader = new FlatFileItemReader<Item>();

	private ExecutionContext executionContext = new ExecutionContext();
	
	protected FlatFileItemReader<String> getReader() {
		return reader;
	}

	protected FlatFileItemReader<Item> getItemReader() {
		return itemReader;
	}

	@Before
	public void setUp() {

		getReader().setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		getReader().setLineMapper(new PassThroughLineMapper());

		getItemReader().setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		getItemReader().setLineMapper(new ItemLineMapper());
	}

	@Test
	public void testRestartWithCustomRecordSeparatorPolicy() throws Exception {

		getReader().setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
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

		getReader().open(executionContext);

		assertEquals("testLine1testLine2", getReader().read());
		assertEquals("testLine3testLine4", getReader().read());

		getReader().update(executionContext);

		getReader().close();

		getReader().open(executionContext);

		assertEquals("testLine5testLine6", getReader().read());
	}

	@Test
	public void testCustomRecordSeparatorPolicyEndOfFile() throws Exception {

		getReader().setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
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

		getReader().setResource(getInputResource("testLine1\ntestLine2\ntestLine3\n"));
		getReader().open(executionContext);

		assertEquals("testLine1testLine2", getReader().read());

		try {
			getReader().read();
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

		getReader().setRecordSeparatorPolicy(new RecordSeparatorPolicy() {

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

		getReader().setResource(getInputResource("testLine1\ntestLine2\ntestLine3\n\n"));
		getReader().open(executionContext);

		assertEquals("testLine1", getReader().read());
		assertEquals("testLine2", getReader().read());
		assertEquals("testLine3", getReader().read());
		assertEquals(null, getReader().read());

	}

	@Test
	public void testCustomRecordSeparatorMultilineBlankLineAfterEnd() throws Exception {

		getReader().setRecordSeparatorPolicy(new RecordSeparatorPolicy() {

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

		getReader().setResource(getInputResource("testLine1\ntestLine2\n\n"));
		getReader().open(executionContext);

		assertEquals("testLine1testLine2", getReader().read());
		assertEquals(null, getReader().read());

	}

	@Test
	public void testRestartWithSkippedLines() throws Exception {

		getReader().setLinesToSkip(2);
		getReader().open(executionContext);

		// read some records
		getReader().read();
		getReader().read();
		// get restart data
		getReader().update(executionContext);
		// read next two records
		getReader().read();
		getReader().read();

		assertEquals(2, executionContext.getInt(ClassUtils.getShortName(getReader().getClass()) + ".read.count"));
		// close input
		getReader().close();

		getReader().setResource(getInputResource("header\nignoreme\ntestLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));

		// init for restart
		getReader().open(executionContext);

		// read remaining records
		assertEquals("testLine3", getReader().read());
		assertEquals("testLine4", getReader().read());

		getReader().update(executionContext);
		assertEquals(4, executionContext.getInt(ClassUtils.getShortName(getReader().getClass()) + ".read.count"));
	}

	@Test
	public void testCurrentItemCount() throws Exception {

		getReader().setCurrentItemCount(2);
		getReader().open(executionContext);

		// read some records
		getReader().read();
		getReader().read();
		// get restart data
		getReader().update(executionContext);

		assertEquals(4, executionContext.getInt(ClassUtils.getShortName(getReader().getClass()) + ".read.count"));
		// close input
		getReader().close();

	}

	@Test
	public void testMaxItemCount() throws Exception {

		getReader().setMaxItemCount(2);
		getReader().open(executionContext);

		// read some records
		getReader().read();
		getReader().read();
		// get restart data
		getReader().update(executionContext);
		assertNull(getReader().read());

		assertEquals(2, executionContext.getInt(ClassUtils.getShortName(getReader().getClass()) + ".read.count"));
		// close input
		getReader().close();

	}

	@Test
	public void testMaxItemCountFromContext() throws Exception {

		getReader().setMaxItemCount(2);
		executionContext.putInt(getReader().getClass().getSimpleName() + ".read.count.max", Integer.MAX_VALUE);
		getReader().open(executionContext);
		// read some records
		getReader().read();
		getReader().read();
		assertNotNull(getReader().read());
		// close input
		getReader().close();

	}

	@Test
	public void testCurrentItemCountFromContext() throws Exception {

		getReader().setCurrentItemCount(2);
		executionContext.putInt(getReader().getClass().getSimpleName() + ".read.count", 3);
		getReader().open(executionContext);
		// read some records
		assertEquals("testLine4", getReader().read());
		// close input
		getReader().close();

	}

	@Test
	public void testMaxAndCurrentItemCount() throws Exception {

		getReader().setMaxItemCount(2);
		getReader().setCurrentItemCount(2);
		getReader().open(executionContext);
		// read some records
		assertNull(getReader().read());
		// close input
		getReader().close();

	}

	@Test
	public void testNonExistentResource() throws Exception {

		Resource resource = new NonExistentResource();

		getReader().setResource(resource);

		// afterPropertiesSet should only throw an exception if the Resource is
		// null
		getReader().afterPropertiesSet();

		getReader().setStrict(false);
		getReader().open(executionContext);
		assertNull(getReader().read());
		getReader().close();
	}

	@Test
	public void testOpenBadIOInput() throws Exception {

		getReader().setResource(new AbstractResource() {
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
			getReader().open(executionContext);
			fail();
		}
		catch (ItemStreamException ex) {
			// expected
		}

		// read() should then return a null
		assertNull(getReader().read());
		getReader().close();

	}

	@Test
	public void testDirectoryResource() throws Exception {

		FileSystemResource resource = new FileSystemResource("target/data");
		resource.getFile().mkdirs();
		assertTrue(resource.getFile().isDirectory());
		getReader().setResource(resource);
		getReader().afterPropertiesSet();

		getReader().setStrict(false);
		getReader().open(executionContext);
		assertNull(getReader().read());

	}

	@Test
	public void testRuntimeFileCreation() throws Exception {

		Resource resource = new NonExistentResource();

		getReader().setResource(resource);

		// afterPropertiesSet should only throw an exception if the Resource is
		// null
		getReader().afterPropertiesSet();

		// replace the resource to simulate runtime resource creation
		getReader().setResource(getInputResource(TEST_STRING));
		getReader().open(executionContext);
		assertEquals(TEST_STRING, getReader().read());
	}

	/**
	 * In strict mode, resource must exist at the time reader is opened.
	 */
	@Test(expected = ItemStreamException.class)
	public void testStrictness() throws Exception {

		Resource resource = new NonExistentResource();

		getReader().setResource(resource);
		getReader().setStrict(true);

		getReader().afterPropertiesSet();

		getReader().open(executionContext);
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
		getReader().setLineMapper(exceptionLineMapper);
		getReader().afterPropertiesSet();

		getReader().open(executionContext);
		assertNotNull(getReader().read());

		try {
			getReader().read();
			fail();
		}
		catch (FlatFileParseException expected) {
			assertEquals(2, expected.getLineNumber());
			assertEquals("testLine2", expected.getInput());
			assertEquals("Couldn't map line 2", expected.getCause().getMessage());
			assertEquals("Parsing error at line: 2 in resource=[resource loaded from byte array], input=[testLine2]", expected.getMessage());
		}
	}

	@Test
	public void testItemCountAware() throws Exception {
		getItemReader().open(executionContext);
		Item item1 = getItemReader().read();
		assertEquals("testLine1", item1.getValue());
		assertEquals(1, item1.getItemCount());
		Item item2 = getItemReader().read();
		assertEquals("testLine2", item2.getValue());
		assertEquals(2, item2.getItemCount());
		getItemReader().update(executionContext);
		getItemReader().close();

		getItemReader().open(executionContext);
		Item item3 = getItemReader().read();
		assertEquals("testLine3", item3.getValue());
		assertEquals(3, item3.getItemCount());
	}

	@Test
	public void testItemCountAwareMultiLine() throws Exception {
		getItemReader().setRecordSeparatorPolicy(new RecordSeparatorPolicy() {

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

		getItemReader().open(executionContext);
		Item item1 = getItemReader().read();
		assertEquals("testLine1testLine2", item1.getValue());
		assertEquals(1, item1.getItemCount());
		Item item2 = getItemReader().read();
		assertEquals("testLine3testLine4", item2.getValue());
		assertEquals(2, item2.getItemCount());
		getItemReader().update(executionContext);
		getItemReader().close();

		getItemReader().open(executionContext);
		Item item3 = getItemReader().read();
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

	protected static class Item implements ItemCountAware {

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
