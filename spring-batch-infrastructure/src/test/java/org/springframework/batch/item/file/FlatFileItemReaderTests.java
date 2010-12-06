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

	private ExecutionContext executionContext = new ExecutionContext();

	@Before
	public void setUp() {

		reader.setResource(getInputResource("testLine1\ntestLine2\ntestLine3\ntestLine4\ntestLine5\ntestLine6"));
		reader.setLineMapper(new PassThroughLineMapper());
	}

	@Test
	public void testRestartWithCustomRecordSeparatorPolicy() throws Exception {

		reader.setRecordSeparatorPolicy(new RecordSeparatorPolicy() {
			// 1 record = 2 lines
			boolean pair = true;

			public boolean isEndOfRecord(String line) {
				pair = !pair;
				return pair;
			}

			public String postProcess(String record) {
				return record;
			}

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

			public boolean isEndOfRecord(String line) {
				pair = !pair;
				return pair;
			}

			public String postProcess(String record) {
				return record;
			}

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

			public boolean isEndOfRecord(String line) {
				return StringUtils.hasText(line);
			}

			public String postProcess(String record) {
				return StringUtils.hasText(record) ? record : null;
			}

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

			public boolean isEndOfRecord(String line) {
				if (StringUtils.hasText(line)) {
					pair = !pair;
				}
				return pair;
			}

			public String postProcess(String record) {
				return StringUtils.hasText(record) ? record : null;
			}

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
			public String getDescription() {
				return null;
			}

			public InputStream getInputStream() throws IOException {
				throw new IOException();
			}

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

		FileSystemResource resource = new FileSystemResource("target/data");
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
			assertEquals("Parsing error at line: 2 in resource=[resource loaded from byte array], input=[testLine2]", expected.getMessage());
		}
	}

	private Resource getInputResource(String input) {
		return new ByteArrayResource(input.getBytes());
	}

	private static class NonExistentResource extends AbstractResource {

		public NonExistentResource() {
		}

		public boolean exists() {
			return false;
		}

		public String getDescription() {
			return "NonExistentResource";
		}

		public InputStream getInputStream() throws IOException {
			return null;
		}
	}
}
