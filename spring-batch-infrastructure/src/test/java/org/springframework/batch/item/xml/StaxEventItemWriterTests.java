package org.springframework.batch.item.xml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import static org.junit.Assert.*;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.oxm.MarshallingEventWriterSerializer;
import org.springframework.core.io.DescriptiveResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.xml.transform.StaxResult;

/**
 * Tests for {@link StaxEventItemWriter}.
 */
public class StaxEventItemWriterTests {

	// object under test
	private StaxEventItemWriter<Object> writer;

	// output file
	private Resource resource;

	private ExecutionContext executionContext;

	// test item for writing to output
	private Object item = new Object() {
		public String toString() {
			return ClassUtils.getShortName(StaxEventItemWriter.class)+"-testString";
		}
	};

	private static final String TEST_STRING = "<!--" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString-->";

	private static final int NOT_FOUND = -1;

	@Before
	public void setUp() throws Exception {
		resource = new FileSystemResource(File.createTempFile("StaxEventWriterOutputSourceTests", "xml"));
		writer = createItemWriter();
		executionContext = new ExecutionContext();
	}

	/**
	 * Flush should pass buffered items to Serializer.
	 */
	@Test
	public void testFlush() throws Exception {
		writer.open(executionContext);
		InputCheckMarshaller marshaller = new InputCheckMarshaller();
		MarshallingEventWriterSerializer<Object> serializer = new MarshallingEventWriterSerializer<Object>(marshaller);
		writer.setSerializer(serializer);

		// see asserts in the marshaller
		writer.write(item);
		assertFalse(marshaller.wasCalled);

		writer.flush();
		assertTrue(marshaller.wasCalled);

	}

	@Test
	public void testClear() throws Exception {
		writer.open(executionContext);
		writer.write(item);
		writer.write(item);
		writer.clear();
		// writer.write(item);
		writer.flush();
		assertFalse(contains(outputFileContent(), TEST_STRING));
	}

	/**
	 * Rolled back records should not be written to output file.
	 */
	@Test
	public void testRollback() throws Exception {
		writer.open(executionContext);
		writer.write(item);
		// rollback
		writer.clear();
		assertFalse(outputFileContent().contains(TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteAndFlush() throws Exception {
		writer.open(executionContext);
		writer.write(item);
		String content = outputFileContent();
		assertFalse(content.contains(TEST_STRING));
		writer.flush();
		content = outputFileContent();
		assertTrue("Wrong content: "+content, contains(content, TEST_STRING));
	}

	/**
	 * Restart scenario - content is appended to the output file after restart.
	 */
	@Test
	public void testRestart() throws Exception {
		writer.open(executionContext);
		// write item
		writer.write(item);
		writer.flush();
		writer.update(executionContext);
		writer.close(executionContext);

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.open(executionContext);
		writer.write(item);
		writer.close(executionContext);

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		String outputFile = outputFileContent();
		int firstRecord = outputFile.indexOf(TEST_STRING);
		int secondRecord = outputFile.indexOf(TEST_STRING, firstRecord + TEST_STRING.length());
		int thirdRecord = outputFile.indexOf(TEST_STRING, secondRecord + TEST_STRING.length());

		// (two records should be written)
		assertTrue(firstRecord != NOT_FOUND);
		assertTrue(secondRecord != NOT_FOUND);
		assertEquals(NOT_FOUND, thirdRecord);
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeader() throws Exception {
		Object header1 = new Object();
		Object header2 = new Object();
		writer.setHeaderItems(new Object[] {header1, header2});
		writer.open(executionContext);
		writer.write(item);
		writer.flush();
		String content = outputFileContent();
		assertTrue("Wrong content: "+content, contains(content, "<!--" + header1 + "-->"));
		assertTrue("Wrong content: "+content, contains(content, "<!--" + header2 + "-->"));
		assertTrue("Wrong content: "+content, contains(content, TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeaderAfterRollback() throws Exception {
		Object header = new Object();
		writer.setHeaderItems(new Object[] {header});
		writer.open(executionContext);
		writer.write(item);
		writer.clear();
		writer.open(executionContext);
		writer.write(item);
		writer.flush();
		String content = outputFileContent();
		assertEquals("Wrong content: "+content, 1, countContains(content, "<!--" + header + "-->"));
		assertEquals("Wrong content: "+content, 1, countContains(content, TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeaderAfterFlushAndRollback() throws Exception {
		Object header = new Object();
		writer.setHeaderItems(new Object[] {header});
		writer.open(executionContext);
		writer.write(item);
		writer.flush();
		writer.update(executionContext);
		writer.close(executionContext);
		writer.open(executionContext);
		writer.write(item);
		writer.clear();
		writer.flush();
		String content = outputFileContent();
		assertEquals("Wrong content: "+content, 1, countContains(content, "<!--" + header + "-->"));
		assertEquals("Wrong content: "+content, 1, countContains(content, TEST_STRING));
	}

	/**
	 * Count of 'records written so far' is returned as statistics.
	 */
	@Test
	public void testStreamContext() throws Exception {
		writer.open(executionContext);
		final int NUMBER_OF_RECORDS = 10;
		for (int i = 1; i <= NUMBER_OF_RECORDS; i++) {
			writer.write(item);
			writer.update(executionContext);
			long writeStatistics = executionContext.getLong(ClassUtils.getShortName(StaxEventItemWriter.class)
					+ ".record.count");

			assertEquals(i, writeStatistics);
		}
	}

	/**
	 * Open method writes the root tag, close method adds corresponding end tag.
	 */
	@Test
	public void testOpenAndClose() throws Exception {
		writer.setRootTagName("testroot");
		writer.setRootElementAttributes(new HashMap<String, String>() {
			{
				put("attribute", "value");
			}
		});
		writer.open(executionContext);
		writer.flush();

		assertTrue(outputFileContent().indexOf("<testroot attribute=\"value\"") != NOT_FOUND);

		writer.close(null);
		assertTrue(outputFileContent().indexOf("<testroot attribute=\"value\">") != NOT_FOUND);
		assertTrue(outputFileContent().endsWith("</testroot>"));
	}
	
	@Test
	public void testNonExistantResource() throws Exception {
		Resource doesntExist = new DescriptiveResource("") {

			public boolean exists() {
				return false;
			}

		};
		writer.setResource(doesntExist);

		try {
			writer.open(executionContext);
			fail();
		}
		catch (IllegalStateException e) {
			assertEquals("Output resource must exist", e.getMessage());
		}
	}

	/**
	 * Checks the received parameters.
	 */
	private class InputCheckMarshaller implements Marshaller {

		boolean wasCalled = false;

		public void marshal(Object graph, Result result) {
			wasCalled = true;
			assertTrue(result instanceof StaxResult);
			assertSame(item, graph);
		}

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}
	}

	/**
	 * Writes object's toString representation as XML comment.
	 */
	private static class SimpleMarshaller implements Marshaller {
		public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
			Assert.isInstanceOf(StaxResult.class, result);

			StaxResult staxResult = (StaxResult) result;
			try {
				staxResult.getXMLEventWriter().add(XMLEventFactory.newInstance().createComment(graph.toString()));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException("Exception while writing to output file", e);
			}
		}

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}
	}

	/**
	 * @return output file content as String
	 */
	private String outputFileContent() throws IOException {
		return FileUtils.readFileToString(resource.getFile(), null);
	}

	/**
	 * @return new instance of fully configured writer
	 */
	private StaxEventItemWriter<Object> createItemWriter() throws Exception {
		StaxEventItemWriter<Object> source = new StaxEventItemWriter<Object>();
		source.setResource(resource);

		Marshaller marshaller = new SimpleMarshaller();
		MarshallingEventWriterSerializer<Object> serializer = new MarshallingEventWriterSerializer<Object>(marshaller);
		source.setSerializer(serializer);

		source.setEncoding("UTF-8");
		source.setRootTagName("root");
		source.setVersion("1.0");
		source.setOverwriteOutput(true);
		source.setSaveState(true);

		source.afterPropertiesSet();

		return source;
	}

	private boolean contains(String str, String searchStr) {
		return str.indexOf(searchStr) != -1;
	}

	private int countContains(String str, String searchStr) {
		int begin = -1;
		int count = 0;
		while (str.indexOf(searchStr, begin+1) > begin) {
			count++;
			begin = str.indexOf(searchStr, begin);
		}
		return count;
	}
}
