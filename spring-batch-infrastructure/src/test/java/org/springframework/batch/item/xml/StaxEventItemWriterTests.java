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
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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
			return ClassUtils.getShortName(StaxEventItemWriter.class) + "-testString";
		}
	};

	private List<? extends Object> items = Collections.singletonList(item);

	private static final String TEST_STRING = "<" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString/>";

	private static final String NS_TEST_STRING = "<ns:" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString/>";

	private static final String FOO_TEST_STRING = "<foo:" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString/>";

	private SimpleMarshaller marshaller;

	@Before
	public void setUp() throws Exception {
		File directory = new File("target/data");
		directory.mkdirs();
		resource = new FileSystemResource(File.createTempFile("StaxEventWriterOutputSourceTests", ".xml", directory));
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
		writer.close();
		String content = getOutputFileContent();
		assertTrue("Wrong content: " + content, content.contains(TEST_STRING));
	}

	@Test
	public void testWriteAndForceFlush() throws Exception {
		writer.setForceSync(true);
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue("Wrong content: " + content, content.contains(TEST_STRING));
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
		writer.close();

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.open(executionContext);
		writer.write(items);
		writer.write(items);
		writer.close();

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		String outputFile = getOutputFileContent();
		assertEquals(3, StringUtils.countOccurrencesOf(outputFile, TEST_STRING));
		assertEquals("<root>" + TEST_STRING + TEST_STRING + TEST_STRING + "</root>", outputFile.replace(" ", ""));
	}

	@Test
	public void testTransactionalRestart() throws Exception {
		writer.open(executionContext);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					// write item
					writer.write(items);
				}
				catch (Exception e) {
					throw new UnexpectedInputException("Could not write data", e);
				}
				// get restart data
				writer.update(executionContext);
				return null;
			}
		});
		writer.close();

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				}
				catch (Exception e) {
					throw new UnexpectedInputException("Could not write data", e);
				}
				// get restart data
				writer.update(executionContext);
				return null;
			}
		});
		writer.close();

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		String outputFile = getOutputFileContent();
		assertEquals(2, StringUtils.countOccurrencesOf(outputFile, TEST_STRING));
		assertTrue(outputFile.contains("<root>" + TEST_STRING + TEST_STRING + "</root>"));
	}

	@Test
	public void testTransactionalRestartFailOnFirstWrite() throws Exception {

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		writer.open(executionContext);
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					}
					catch (Exception e) {
						throw new IllegalStateException("Could not write data", e);
					}
					throw new UnexpectedInputException("Could not write data");
				}
			});
		}
		catch (UnexpectedInputException e) {
			// expected
		}
		writer.close();
		System.err.println(getOutputFileContent());
		String outputFile = getOutputFileContent();
		assertEquals("<root></root>", outputFile);

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				writer.open(executionContext);
				try {
					writer.write(items);
				}
				catch (Exception e) {
					throw new UnexpectedInputException("Could not write data", e);
				}
				// get restart data
				writer.update(executionContext);
				return null;
			}
		});
		writer.close();

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		outputFile = getOutputFileContent();
		System.err.println(getOutputFileContent());
		assertEquals(1, StringUtils.countOccurrencesOf(outputFile, TEST_STRING));
		assertTrue(outputFile.contains("<root>" + TEST_STRING + "</root>"));
		assertEquals("<root><StaxEventItemWriter-testString/></root>", outputFile);
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeader() throws Exception {

		writer.setHeaderCallback(new StaxWriterCallback() {

			public void write(XMLEventWriter writer) throws IOException {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				try {
					writer.add(factory.createStartElement("", "", "header"));
					writer.add(factory.createEndElement("", "", "header"));
				}
				catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}

			}

		});
		writer.open(executionContext);
		writer.write(items);
		String content = getOutputFileContent();
		assertTrue("Wrong content: " + content, content.contains(("<header/>")));
		assertTrue("Wrong content: " + content, content.contains(TEST_STRING));
	}

	/**
	 * Count of 'records written so far' is returned as statistics.
	 */
	@Test
	public void testStreamContext() throws Exception {
		writer.open(executionContext);
		final int NUMBER_OF_RECORDS = 10;
		assertFalse(executionContext.containsKey(ClassUtils.getShortName(StaxEventItemWriter.class) + ".record.count"));
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
		writer.setHeaderCallback(new StaxWriterCallback() {

			public void write(XMLEventWriter writer) throws IOException {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				try {
					writer.add(factory.createStartElement("", "", "header"));
					writer.add(factory.createEndElement("", "", "header"));
				}
				catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}

			}

		});
		writer.setFooterCallback(new StaxWriterCallback() {

			public void write(XMLEventWriter writer) throws IOException {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				try {
					writer.add(factory.createStartElement("", "", "footer"));
					writer.add(factory.createEndElement("", "", "footer"));
				}
				catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}

			}

		});
		writer.setRootTagName("testroot");
		writer.setRootElementAttributes(Collections.<String, String> singletonMap("attribute", "value"));
		writer.open(executionContext);
		writer.close();
		String content = getOutputFileContent();

		assertTrue(content.contains("<testroot attribute=\"value\">"));
		assertTrue(content.contains("<header/>"));
		assertTrue(content.contains("<footer/>"));
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
	 * Item is written to the output file with namespace.
	 */
	@Test
	public void testWriteRootTagWithNamespace() throws Exception {
		writer.setRootTagName("{http://www.springframework.org/test}root");
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue("Wrong content: " + content, content
				.contains(("<root xmlns=\"http://www.springframework.org/test\">")));
		assertTrue("Wrong content: " + content, content.contains(TEST_STRING));
		assertTrue("Wrong content: " + content, content.contains(("</root>")));
	}

	/**
	 * Item is written to the output file with namespace and prefix.
	 */
	@Test
	public void testWriteRootTagWithNamespaceAndPrefix() throws Exception {
		writer.setRootTagName("{http://www.springframework.org/test}ns:root");
		writer.afterPropertiesSet();
		marshaller.setNamespace(writer.getRootTagNamespace());
		marshaller.setNamespacePrefix(writer.getRootTagNamespacePrefix());
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue("Wrong content: " + content, content
				.contains(("<ns:root xmlns:ns=\"http://www.springframework.org/test\">")));
		assertTrue("Wrong content: " + content, content.contains(NS_TEST_STRING));
		assertTrue("Wrong content: " + content, content.contains(("</ns:root>")));
		assertTrue("Wrong content: " + content, content.contains(("<ns:root")));
	}

	/**
	 * Item is written to the output file with additional namespaces and prefix.
	 */
	@Test
	public void testWriteRootTagWithAdditionalNamespace() throws Exception {
		writer.setRootTagName("{http://www.springframework.org/test}ns:root");
		marshaller.setNamespace("urn:org.test.foo");
		marshaller.setNamespacePrefix("foo");
		writer.setRootElementAttributes(Collections.singletonMap("xmlns:foo", "urn:org.test.foo"));
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue("Wrong content: " + content, content
				.contains(("<ns:root xmlns:ns=\"http://www.springframework.org/test\" "
						+ "xmlns:foo=\"urn:org.test.foo\">")));
		assertTrue("Wrong content: " + content, content.contains(FOO_TEST_STRING));
		assertTrue("Wrong content: " + content, content.contains(("</ns:root>")));
		assertTrue("Wrong content: " + content, content.contains(("<ns:root")));
	}

	/**
	 * Writes object's toString representation as XML comment.
	 */
	private static class SimpleMarshaller implements Marshaller {

		private String namespacePrefix = "";

		private String namespace = "";

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}

		public void setNamespacePrefix(String namespacePrefix) {
			this.namespacePrefix = namespacePrefix;
		}

		public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
 	 	 Assert.isInstanceOf( Result.class, result);
			try {
				StaxUtils.getXmlEventWriter( result ).add( XMLEventFactory.newInstance().createStartElement(namespacePrefix, namespace, graph.toString()));
				StaxUtils.getXmlEventWriter( result ).add( XMLEventFactory.newInstance().createEndElement(namespacePrefix, namespace, graph.toString()));
			}
			catch ( Exception e) {
				throw new RuntimeException("Exception while writing to output file", e);
			}
		}

		@SuppressWarnings("rawtypes")
		public boolean supports(Class clazz) {
			return true;
		}
	}

	/**
	 * @return output file content as String
	 */
	private String getOutputFileContent() throws IOException {
		String value = FileUtils.readFileToString(resource.getFile(), null);
		value = value.replace("<?xml version='1.0' encoding='UTF-8'?>", "");
		return value;
	}

	/**
	 * @return new instance of fully configured writer
	 */
	private StaxEventItemWriter<Object> createItemWriter() throws Exception {
		StaxEventItemWriter<Object> source = new StaxEventItemWriter<Object>();
		source.setResource(resource);

		marshaller = new SimpleMarshaller();
		source.setMarshaller(marshaller);

		source.setEncoding("UTF-8");
		source.setRootTagName("root");
		source.setVersion("1.0");
		source.setOverwriteOutput(true);
		source.setSaveState(true);

		source.afterPropertiesSet();

		return source;
	}

}
