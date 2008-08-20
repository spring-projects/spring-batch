package org.springframework.batch.item.xml;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.oxm.MarshallingEventWriterSerializer;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;
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

	private List<? extends Object> items = Collections.singletonList(item);

	private static final String TEST_STRING = "<!--" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString-->";

	@Before
	public void setUp() throws Exception {
		resource = new FileSystemResource(File.createTempFile("StaxEventWriterOutputSourceTests", ".xml"));
		writer = createItemWriter();
		executionContext = new ExecutionContext();
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteAndFlush() throws Exception {
		writer.open(executionContext);
		writer.write(items);
		writer.close(executionContext);
		String content = outputFileContent();
		assertTrue("Wrong content: "+content, content.contains(TEST_STRING));
	}

	/**
	 * Restart scenario - content is appended to the output file after restart.
	 */
	@Test
	public void testRestart() throws Exception {
		writer.open(executionContext);
		// write item
		writer.write(items);
		writer.update(executionContext);
		writer.close(executionContext);

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.open(executionContext);
		writer.write(items);
		writer.close(executionContext);

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		String outputFile = outputFileContent();
		
		assertEquals(2, StringUtils.countOccurrencesOf(outputFile, TEST_STRING));
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
		writer.write(items);
		String content = outputFileContent();
		assertTrue("Wrong content: "+content, content.contains(("<!--" + header1 + "-->")));
		assertTrue("Wrong content: "+content, content.contains(("<!--" + header2 + "-->")));
		assertTrue("Wrong content: "+content, content.contains(TEST_STRING));
	}

	/**
	 * Count of 'records written so far' is returned as statistics.
	 */
	@Test
	public void testStreamContext() throws Exception {
		writer.open(executionContext);
		final int NUMBER_OF_RECORDS = 10;
		assertFalse(executionContext.containsKey(ClassUtils.getShortName(StaxEventItemWriter.class)
				+ ".record.count"));
		for (int i = 1; i <= NUMBER_OF_RECORDS; i++) {
			writer.write(items);
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
		writer.close(null);
		String content = outputFileContent();
		assertTrue(content.contains("<testroot attribute=\"value\">"));
		assertTrue(content.endsWith("</testroot>"));
	}
	
	@Test
	public void testNonExistantResource() throws Exception {
		Resource doesntExist = createMock(Resource.class);
		expect(doesntExist.getFile()).andReturn(File.createTempFile("arbitrary", null));
		expect(doesntExist.exists()).andReturn(false);
		replay(doesntExist);
		
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
}
