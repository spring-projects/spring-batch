package org.springframework.batch.io.xml;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.springframework.batch.io.xml.oxm.MarshallingEventWriterSerializer;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.util.Assert;
import org.springframework.xml.transform.StaxResult;

/**
 * Tests for {@link StaxStreamWriterOutputSource}.
 */
public class StaxEventWriterItemWriterTests extends TestCase {

	// object under test
	private StaxEventItemWriter writer;

	// output file
	private Resource resource;

	// test record for writing to output
	private Object record = new Object() {
		public String toString() {
			return TEST_STRING;
		}
	};

	private static final String TEST_STRING = "StaxEventWriterOutputSourceTests-testString";

	private static final int NOT_FOUND = -1;

	protected void setUp() throws Exception {
		resource = new FileSystemResource(File.createTempFile("StaxEventWriterOutputSourceTests", "xml"));
		writer = createItemWriter();
		writer.open();
	}

	/**
	 * Write should pass its argument and StaxResult object to Serializer
	 */
	public void testWrite() throws Exception {
		Marshaller marshaller = new InputCheckMarshaller();
		MarshallingEventWriterSerializer serializer = new MarshallingEventWriterSerializer(marshaller);
		writer.setSerializer(serializer);

		// see asserts in the marshaller
		writer.write(record);

	}

	/**
	 * Rolled back records should not be written to output file.
	 */
	public void testRollback() throws Exception {
		writer.write(record);
		// rollback
		writer.reset();
		assertEquals("", outputFileContent());
	}

	/**
	 * Commited output is written to the output file.
	 */
	public void testCommit() throws Exception {
		writer.write(record);
		// commit
		writer.mark();
		assertTrue(outputFileContent().contains(TEST_STRING));
	}

	/**
	 * Restart scenario - content is appended to the output file after restart.
	 */
	public void testRestart() throws Exception {
		// write record
		writer.write(record);
		// writer.mark();
		ExecutionAttributes streamContext = writer.getExecutionAttributes();
		writer.close();

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.restoreFrom(streamContext);
		writer.write(record);
		writer.close();
		
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
	 * Count of 'records written so far' is returned as statistics.
	 */
	public void testStreamContext() throws Exception {
		final int NUMBER_OF_RECORDS = 10;
		for (int i = 1; i <= NUMBER_OF_RECORDS; i++) {
			writer.write(record);
			long writeStatistics = writer.getExecutionAttributes().getLong(StaxEventItemWriter.WRITE_STATISTICS_NAME);

			assertEquals(i, writeStatistics);
		}
	}

	/**
	 * Open method writes the root tag, close method adds corresponding end tag.
	 */
	public void testOpenAndClose() throws IOException {
		writer.setRootTagName("testroot");
		writer.setRootElementAttributes(new HashMap() {
			{
				put("attribute", "value");
			}
		});
		writer.open();
		writer.mark();

		assertTrue(outputFileContent().indexOf("<testroot attribute=\"value\"") != NOT_FOUND);

		writer.close();
		assertTrue(outputFileContent().endsWith("</testroot>"));
	}

	/**
	 * Checks the received parameters.
	 */
	private class InputCheckMarshaller implements Marshaller {
		public void marshal(Object graph, Result result) {
			assertTrue(result instanceof StaxResult);
			assertSame(record, graph);
		}

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
	private StaxEventItemWriter createItemWriter() throws Exception {
		StaxEventItemWriter source = new StaxEventItemWriter();
		source.setResource(resource);

		Marshaller marshaller = new SimpleMarshaller();
		MarshallingEventWriterSerializer serializer = new MarshallingEventWriterSerializer(marshaller);
		source.setSerializer(serializer);

		source.setEncoding("UTF-8");
		source.setRootTagName("root");
		source.setVersion("1.0");
		source.setOverwriteOutput(true);

		source.afterPropertiesSet();
		
		return source;
	}
}
