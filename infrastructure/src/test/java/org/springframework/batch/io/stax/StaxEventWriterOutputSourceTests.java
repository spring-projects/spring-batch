package org.springframework.batch.io.stax;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.springframework.batch.io.oxm.MarshallingObjectToXmlSerializer;
import org.springframework.batch.restart.RestartData;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.util.Assert;
import org.springframework.xml.transform.StaxResult;

/**
 * Tests for {@link StaxStreamWriterOutputSource}.
 */
public class StaxEventWriterOutputSourceTests extends TestCase {

	// object under test
	private StaxEventWriterOutputSource source;

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
		source = newOutputSource();
	}

	/**
	 * Write should pass its argument and StaxResult object to Serializer
	 */
	public void testWrite() throws Exception {
		Marshaller marshaller = new InputCheckMarshaller();
		MarshallingObjectToXmlSerializer serializer = new MarshallingObjectToXmlSerializer(marshaller);
		source.setSerializer(serializer);

		// see asserts in the marshaller
		source.write(record);

	}

	/**
	 * Rolled back records should not be written to output file.
	 */
	public void testRollback() throws Exception {
		source.write(record);

		// rollback
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
		assertEquals("", outputFileContent());
	}

	/**
	 * Commited output is written to the output file.
	 */
	public void testCommit() throws Exception {
		source.write(record);

		// commit
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		assertTrue(outputFileContent().indexOf(TEST_STRING) != NOT_FOUND);
	}

	/**
	 * Restart scenario - content is appended to the output file after restart.
	 */
	public void testRestart() throws Exception {
		// write records
		source.write(record);
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		RestartData restartData = source.getRestartData();

		// create new output source from saved restart data and continue writing
		source = newOutputSource();
		source.restoreFrom(restartData);
		source.write(record);
		source.close();

		// check the output is concatenation of 'before restart' and 'after restart' writes.
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
	public void testStatistics() throws Exception {
		final int NUMBER_OF_RECORDS = 10;
		for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
			String writeStatistics =
				source.getStatistics().getProperty(StaxEventWriterOutputSource.WRITE_STATISTICS_NAME);

			assertEquals(String.valueOf(i), writeStatistics);
			source.write(record);
		}
	}

	/**
	 * Open method writes the root tag, close method adds corresponding end tag.
	 */
	public void testOpenAndClose() throws IOException {
		source.setRootTagName("testroot");
		source.setRootElementAttributes(new HashMap() {{
			put("attribute", "value");
		}});
		source.open();
		source.getSynchronization().afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

		assertTrue(outputFileContent().indexOf("<testroot attribute=\"value\"") != NOT_FOUND);

		source.close();
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
	 * @return new instance of fully configured output source
	 */
	private StaxEventWriterOutputSource newOutputSource() {
		StaxEventWriterOutputSource source = new StaxEventWriterOutputSource();
		source.setResource(resource);

		Marshaller marshaller = new SimpleMarshaller();
		MarshallingObjectToXmlSerializer serializer = new MarshallingObjectToXmlSerializer(marshaller);
		source.setSerializer(serializer);

		return source;
	}
}
