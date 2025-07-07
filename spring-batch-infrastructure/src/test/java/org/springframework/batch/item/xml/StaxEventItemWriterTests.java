/*
 * Copyright 2008-2025 the original author or authors.
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
package org.springframework.batch.item.xml;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.WriterNotOpenException;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StaxEventItemWriter}.
 *
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 * @author Elimelec Burghelea
 */
class StaxEventItemWriterTests {

	// object under test
	private StaxEventItemWriter<Object> writer;

	// output file
	private WritableResource resource;

	private ExecutionContext executionContext;

	// test item for writing to output
	private final Object item = new Object() {
		@Override
		public String toString() {
			return ClassUtils.getShortName(StaxEventItemWriter.class) + "-testString";
		}
	};

	private final JAXBItem jaxbItem = new JAXBItem();

	// test item for writing to output with multi byte character
	private final Object itemMultiByte = new Object() {
		@Override
		public String toString() {
			return ClassUtils.getShortName(StaxEventItemWriter.class) + "-téstStrïng";
		}
	};

	private final Chunk<?> items = Chunk.of(item);

	private final Chunk<?> itemsMultiByte = Chunk.of(itemMultiByte);

	private final Chunk<?> jaxbItems = Chunk.of(jaxbItem);

	private static final String TEST_STRING = "<" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString/>";

	private static final String TEST_STRING_MULTI_BYTE = "<" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-téstStrïng/>";

	private static final String NS_TEST_STRING = "<ns:" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString/>";

	private static final String FOO_TEST_STRING = "<foo:" + ClassUtils.getShortName(StaxEventItemWriter.class)
			+ "-testString/>";

	private SimpleMarshaller marshaller;

	private Jaxb2Marshaller jaxbMarshaller;

	@BeforeEach
	void setUp() throws Exception {
		File directory = new File("target/data");
		directory.mkdirs();
		resource = new FileSystemResource(File.createTempFile("StaxEventWriterOutputSourceTests", ".xml", directory));
		writer = createItemWriter();
		executionContext = new ExecutionContext();
		jaxbMarshaller = new Jaxb2Marshaller();
		jaxbMarshaller.setClassesToBeBound(JAXBItem.class);
	}

	/**
	 * Test setting writer name.
	 */
	@Test
	void testSetName() throws Exception {
		writer.setName("test");
		writer.open(executionContext);
		writer.write(items);
		writer.update(executionContext);
		writer.close();
		assertTrue(executionContext.containsKey("test.position"),
				"execution context keys should be prefixed with writer name");
	}

	@Test
	void testAssertWriterIsInitialized() {
		StaxEventItemWriter<String> writer = new StaxEventItemWriter<>();

		assertThrows(WriterNotOpenException.class, () -> writer.write(Chunk.of("foo")));
	}

	@Test
	void testStandaloneDeclarationInHeaderWhenNotSet() throws Exception {
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent(writer.getEncoding(), false);
		assertFalse(content.contains("standalone="));
	}

	@Test
	void testStandaloneDeclarationInHeaderWhenSetToTrue() throws Exception {
		writer.setStandalone(true);
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent(writer.getEncoding(), false);
		assertTrue(content.contains("standalone='yes'"));
	}

	@Test
	void testStandaloneDeclarationInHeaderWhenSetToFalse() throws Exception {
		writer.setStandalone(false);
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent(writer.getEncoding(), false);
		assertTrue(content.contains("standalone='no'"));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	void testWriteAndFlush() throws Exception {
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	@Test
	void testWriteAndForceFlush() throws Exception {
		writer.setForceSync(true);
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	/**
	 * Restart scenario - content is appended to the output file after restart.
	 */
	@Test
	void testRestart() throws Exception {
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
	void testTransactionalRestart() throws Exception {
		writer.open(executionContext);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
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
		});
		writer.close();

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write(items);
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		writer.close();

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		String outputFile = getOutputFileContent();
		assertEquals(2, StringUtils.countOccurrencesOf(outputFile, TEST_STRING));
		assertTrue(outputFile.contains("<root>" + TEST_STRING + TEST_STRING + "</root>"));
	}

	@Test
	// BATCH-1959
	void testTransactionalRestartWithMultiByteCharacterUTF8() throws Exception {
		testTransactionalRestartWithMultiByteCharacter("UTF-8");
	}

	@Test
	// BATCH-1959
	void testTransactionalRestartWithMultiByteCharacterUTF16BE() throws Exception {
		testTransactionalRestartWithMultiByteCharacter("UTF-16BE");
	}

	private void testTransactionalRestartWithMultiByteCharacter(String encoding) throws Exception {
		writer.setEncoding(encoding);
		writer.open(executionContext);

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				// write item
				writer.write(itemsMultiByte);
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		writer.close();

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		writer.setEncoding(encoding);
		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			try {
				writer.write(itemsMultiByte);
			}
			catch (Exception e) {
				throw new UnexpectedInputException("Could not write data", e);
			}
			// get restart data
			writer.update(executionContext);
			return null;
		});
		writer.close();

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		String outputFile = getOutputFileContent(encoding);
		assertEquals(2, StringUtils.countOccurrencesOf(outputFile, TEST_STRING_MULTI_BYTE));
		assertTrue(outputFile.contains("<root>" + TEST_STRING_MULTI_BYTE + TEST_STRING_MULTI_BYTE + "</root>"));
	}

	@Test
	void testTransactionalRestartFailOnFirstWrite() throws Exception {

		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

		writer.open(executionContext);
		try {
			new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
				try {
					writer.write(items);
				}
				catch (Exception e) {
					throw new IllegalStateException("Could not write data", e);
				}
				throw new UnexpectedInputException("Could not write data");
			});
		}
		catch (UnexpectedInputException e) {
			// expected
		}
		writer.close();
		String outputFile = getOutputFileContent();
		assertEquals("<root></root>", outputFile);

		// create new writer from saved restart data and continue writing
		writer = createItemWriter();
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
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
		});
		writer.close();

		// check the output is concatenation of 'before restart' and 'after
		// restart' writes.
		outputFile = getOutputFileContent();
		assertEquals(1, StringUtils.countOccurrencesOf(outputFile, TEST_STRING));
		assertTrue(outputFile.contains("<root>" + TEST_STRING + "</root>"));
		assertEquals("<root><StaxEventItemWriter-testString/></root>", outputFile);
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	void testWriteWithHeader() throws Exception {

		writer.setHeaderCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "header"));
				writer.add(factory.createEndElement("", "", "header"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.open(executionContext);
		writer.write(items);
		String content = getOutputFileContent();
		assertTrue(content.contains("<header/>"), "Wrong content: " + content);
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	/**
	 * Count of 'records written so far' is returned as statistics.
	 */
	@Test
	void testStreamContext() throws Exception {
		writer.open(executionContext);
		final int NUMBER_OF_RECORDS = 10;
		assertFalse(executionContext.containsKey(ClassUtils.getShortName(StaxEventItemWriter.class) + ".record.count"));
		for (int i = 1; i <= NUMBER_OF_RECORDS; i++) {
			writer.write(items);
			writer.update(executionContext);
			long writeStatistics = executionContext
				.getLong(ClassUtils.getShortName(StaxEventItemWriter.class) + ".record.count");

			assertEquals(i, writeStatistics);
		}
	}

	/**
	 * Open method writes the root tag, close method adds corresponding end tag.
	 */
	@Test
	void testOpenAndClose() throws Exception {
		writer.setHeaderCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "header"));
				writer.add(factory.createEndElement("", "", "header"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.setFooterCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "footer"));
				writer.add(factory.createEndElement("", "", "footer"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.setRootTagName("testroot");
		writer.setRootElementAttributes(Collections.<String, String>singletonMap("attribute", "value"));
		writer.open(executionContext);
		writer.close();
		String content = getOutputFileContent();

		assertTrue(content.contains("<testroot attribute=\"value\">"));
		assertTrue(content.contains("<header/>"));
		assertTrue(content.contains("<footer/>"));
		assertTrue(content.endsWith("</testroot>"));
	}

	@Test
	void testNonExistantResource() throws Exception {
		WritableResource doesntExist = mock();
		when(doesntExist.getFile()).thenReturn(File.createTempFile("arbitrary", null));
		when(doesntExist.exists()).thenReturn(false);

		writer.setResource(doesntExist);

		Exception exception = assertThrows(IllegalStateException.class, () -> writer.open(executionContext));
		assertEquals("Output resource must exist", exception.getMessage());
	}

	/**
	 * Resource is not deleted when items have been written and shouldDeleteIfEmpty flag
	 * is set.
	 */
	@Test
	void testDeleteIfEmptyRecordsWritten() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	/**
	 * Resource is deleted when no items have been written and shouldDeleteIfEmpty flag is
	 * set.
	 */
	@Test
	void testDeleteIfEmptyNoRecordsWritten() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.close();
		assertFalse(resource.getFile().exists(), "file should be deleted" + resource);
	}

	/**
	 * Resource is deleted when items have not been written and shouldDeleteIfEmpty flag
	 * is set.
	 */
	@Test
	void testDeleteIfEmptyNoRecordsWrittenHeaderAndFooter() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.setHeaderCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "header"));
				writer.add(factory.createEndElement("", "", "header"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.setFooterCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "footer"));
				writer.add(factory.createEndElement("", "", "footer"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.open(executionContext);
		writer.close();
		assertFalse(resource.getFile().exists(), "file should be deleted" + resource);
	}

	/**
	 * Resource is not deleted when items have been written and shouldDeleteIfEmpty flag
	 * is set.
	 */
	@Test
	void testDeleteIfEmptyRecordsWrittenRestart() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.write(items);
		writer.update(executionContext);
		writer.close();

		writer = createItemWriter();
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	/**
	 * Test that the writer can restart if the previous execution deleted empty file.
	 */
	@Test
	void testDeleteIfEmptyRestartAfterDelete() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.update(executionContext);
		writer.close();
		assertFalse(resource.getFile().exists());
		writer = createItemWriter();
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);
		writer.write(items);
		writer.update(executionContext);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	/**
	 * Resource is not deleted when items have been written and shouldDeleteIfEmpty flag
	 * is set (restart after delete).
	 */
	@Test
	void testDeleteIfEmptyNoRecordsWrittenHeaderAndFooterRestartAfterDelete() throws Exception {
		writer.setShouldDeleteIfEmpty(true);
		writer.setHeaderCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "header"));
				writer.add(factory.createEndElement("", "", "header"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.setFooterCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "footer"));
				writer.add(factory.createEndElement("", "", "footer"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.open(executionContext);
		writer.update(executionContext);
		writer.close();
		assertFalse(resource.getFile().exists(), "file should be deleted" + resource);
		writer.open(executionContext);
		writer.write(items);
		writer.update(executionContext);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
	}

	/**
	 * Item is written to the output file with namespace.
	 */
	@Test
	void testWriteRootTagWithNamespace() throws Exception {
		writer.setRootTagName("{https://www.springframework.org/test}root");
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains("<root xmlns=\"https://www.springframework.org/test\">"),
				"Wrong content: " + content);
		assertTrue(content.contains(TEST_STRING), "Wrong content: " + content);
		assertTrue(content.contains("</root>"), "Wrong content: " + content);
	}

	/**
	 * Item is written to the output file with namespace and prefix.
	 */
	@Test
	void testWriteRootTagWithNamespaceAndPrefix() throws Exception {
		writer.setRootTagName("{https://www.springframework.org/test}ns:root");
		writer.afterPropertiesSet();
		marshaller.setNamespace(writer.getRootTagNamespace());
		marshaller.setNamespacePrefix(writer.getRootTagNamespacePrefix());
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains("<ns:root xmlns:ns=\"https://www.springframework.org/test\">"),
				"Wrong content: " + content);
		assertTrue(content.contains(NS_TEST_STRING), "Wrong content: " + content);
		assertTrue(content.contains("</ns:root>"), "Wrong content: " + content);
		assertTrue(content.contains("<ns:root"), "Wrong content: " + content);
	}

	/**
	 * Item is written to the output file with additional namespaces and prefix.
	 */
	@Test
	void testWriteRootTagWithAdditionalNamespace() throws Exception {
		writer.setRootTagName("{https://www.springframework.org/test}ns:root");
		marshaller.setNamespace("urn:org.test.foo");
		marshaller.setNamespacePrefix("foo");
		writer.setRootElementAttributes(Collections.singletonMap("xmlns:foo", "urn:org.test.foo"));
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();
		assertTrue(content.contains(
				"<ns:root xmlns:ns=\"https://www.springframework.org/test\" " + "xmlns:foo=\"urn:org.test.foo\">"),
				"Wrong content: " + content);
		assertTrue(content.contains(FOO_TEST_STRING), "Wrong content: " + content);
		assertTrue(content.contains("</ns:root>"), "Wrong content: " + content);
		assertTrue(content.contains("<ns:root"), "Wrong content: " + content);
	}

	/**
	 * Namespace prefixes are properly initialized on restart.
	 */
	@Test
	void testRootTagWithNamespaceRestart() throws Exception {
		writer.setMarshaller(jaxbMarshaller);
		writer.setRootTagName("{https://www.springframework.org/test}root");
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(jaxbItems);
		writer.update(executionContext);
		writer.close();

		writer = createItemWriter();
		writer.setMarshaller(jaxbMarshaller);
		writer.setRootTagName("{https://www.springframework.org/test}root");
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(jaxbItems);
		writer.update(executionContext);
		writer.close();

		String content = getOutputFileContent();
		assertEquals("<root xmlns=\"https://www.springframework.org/test\"><item/><item/></root>", content,
				"Wrong content: " + content);
	}

	/**
	 * Namespace prefixes are properly initialized on restart.
	 */
	@Test
	void testRootTagWithNamespaceAndPrefixRestart() throws Exception {
		writer.setMarshaller(jaxbMarshaller);
		writer.setRootTagName("{https://www.springframework.org/test}ns:root");
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(jaxbItems);
		writer.update(executionContext);
		writer.close();

		writer = createItemWriter();
		writer.setMarshaller(jaxbMarshaller);
		writer.setRootTagName("{https://www.springframework.org/test}ns:root");
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(jaxbItems);
		writer.update(executionContext);
		writer.close();

		String content = getOutputFileContent();
		assertEquals("<ns:root xmlns:ns=\"https://www.springframework.org/test\"><ns:item/><ns:item/></ns:root>",
				content, "Wrong content: " + content);
	}

	/**
	 * Namespace prefixes are properly initialized on restart.
	 */
	@Test
	void testRootTagWithAdditionalNamespaceRestart() throws Exception {
		writer.setMarshaller(jaxbMarshaller);
		writer.setRootTagName("{urn:org.test.foo}foo:root");
		writer.setRootElementAttributes(Collections.singletonMap("xmlns:ns", "https://www.springframework.org/test"));
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(jaxbItems);
		writer.update(executionContext);
		writer.close();

		writer = createItemWriter();
		writer.setMarshaller(jaxbMarshaller);
		writer.setRootTagName("{urn:org.test.foo}foo:root");
		writer.setRootElementAttributes(Collections.singletonMap("xmlns:ns", "https://www.springframework.org/test"));
		writer.afterPropertiesSet();
		writer.open(executionContext);
		writer.write(jaxbItems);
		writer.update(executionContext);
		writer.close();

		String content = getOutputFileContent();
		assertEquals(
				"<foo:root xmlns:foo=\"urn:org.test.foo\" xmlns:ns=\"https://www.springframework.org/test\"><ns:item/><ns:item/></foo:root>",
				content, "Wrong content: " + content);
	}

	/**
	 * Test with OXM Marshaller that closes the XMLEventWriter.
	 */
	// BATCH-2054
	@Test
	void testMarshallingClosingEventWriter() throws Exception {
		writer.setMarshaller(new SimpleMarshaller() {
			@Override
			public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
				super.marshal(graph, result);
				try {
					StaxTestUtils.getXmlEventWriter(result).close();
				}
				catch (Exception e) {
					throw new RuntimeException("Exception while writing to output file", e);
				}
			}

		});
		writer.afterPropertiesSet();

		writer.open(executionContext);

		writer.write(items);
		writer.write(items);
	}

	/**
	 * Test opening and closing corresponding tags in header- and footer callback.
	 */
	@Test
	void testOpenAndCloseTagsInCallbacks() throws Exception {
		initWriterForSimpleCallbackTests();
		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();

		assertEquals(
				"<ns:testroot xmlns:ns=\"https://www.springframework.org/test\"><ns:group><StaxEventItemWriter-testString/></ns:group></ns:testroot>",
				content, "Wrong content: " + content);
	}

	/**
	 * Test opening and closing corresponding tags in header- and footer callback
	 * (restart).
	 */
	@Test
	void testOpenAndCloseTagsInCallbacksRestart() throws Exception {
		initWriterForSimpleCallbackTests();
		writer.open(executionContext);
		writer.write(items);
		writer.update(executionContext);

		initWriterForSimpleCallbackTests();

		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();

		assertEquals("<ns:testroot xmlns:ns=\"https://www.springframework.org/test\">"
				+ "<ns:group><StaxEventItemWriter-testString/><StaxEventItemWriter-testString/></ns:group></ns:testroot>",
				content, "Wrong content: " + content);
	}

	/**
	 * Test opening and closing corresponding tags in complex header- and footer callback
	 * (restart).
	 */
	@Test
	void testOpenAndCloseTagsInComplexCallbacksRestart() throws Exception {
		initWriterForComplexCallbackTests();
		writer.open(executionContext);
		writer.write(items);
		writer.update(executionContext);

		initWriterForComplexCallbackTests();

		writer.open(executionContext);
		writer.write(items);
		writer.close();
		String content = getOutputFileContent();

		assertEquals("<ns:testroot xmlns:ns=\"https://www.springframework.org/test\">"
				+ "<preHeader>PRE-HEADER</preHeader><ns:group><subGroup><postHeader>POST-HEADER</postHeader>"
				+ "<StaxEventItemWriter-testString/><StaxEventItemWriter-testString/>"
				+ "<preFooter>PRE-FOOTER</preFooter></subGroup></ns:group><postFooter>POST-FOOTER</postFooter>"
				+ "</ns:testroot>", content, "Wrong content: " + content);
	}

	/**
	 * Tests that if file.delete() returns false, an appropriate exception is thrown to
	 * indicate the deletion attempt failed.
	 */
	@Test
	void testFailedFileDeletionThrowsException() throws IOException {
		File mockedFile = spy(resource.getFile());
		writer.setResource(new FileSystemResource(mockedFile));
		writer.setShouldDeleteIfEmpty(true);
		writer.open(executionContext);

		when(mockedFile.delete()).thenReturn(false);

		ItemStreamException exception = assertThrows(ItemStreamException.class, () -> writer.close(),
				"Expected exception when file deletion fails");

		assertEquals("Failed to delete empty file on close", exception.getMessage(), "Wrong exception message");
		assertNotNull(exception.getCause(), "Exception should have a cause");
	}

	private void initWriterForSimpleCallbackTests() throws Exception {
		writer = createItemWriter();
		writer.setHeaderCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("ns", "https://www.springframework.org/test", "group"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		});
		writer.setFooterCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createEndElement("ns", "https://www.springframework.org/test", "group"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.setRootTagName("{https://www.springframework.org/test}ns:testroot");
		writer.afterPropertiesSet();
	}

	// more complex callbacks, writing element before and after the multiple corresponding
	// header- and footer elements
	private void initWriterForComplexCallbackTests() throws Exception {
		writer = createItemWriter();
		writer.setHeaderCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "preHeader"));
				writer.add(factory.createCharacters("PRE-HEADER"));
				writer.add(factory.createEndElement("", "", "preHeader"));
				writer.add(factory.createStartElement("ns", "https://www.springframework.org/test", "group"));
				writer.add(factory.createStartElement("", "", "subGroup"));
				writer.add(factory.createStartElement("", "", "postHeader"));
				writer.add(factory.createCharacters("POST-HEADER"));
				writer.add(factory.createEndElement("", "", "postHeader"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}
		});
		writer.setFooterCallback(writer -> {
			XMLEventFactory factory = XMLEventFactory.newInstance();
			try {
				writer.add(factory.createStartElement("", "", "preFooter"));
				writer.add(factory.createCharacters("PRE-FOOTER"));
				writer.add(factory.createEndElement("", "", "preFooter"));
				writer.add(factory.createEndElement("", "", "subGroup"));
				writer.add(factory.createEndElement("ns", "https://www.springframework.org/test", "group"));
				writer.add(factory.createStartElement("", "", "postFooter"));
				writer.add(factory.createCharacters("POST-FOOTER"));
				writer.add(factory.createEndElement("", "", "postFooter"));
			}
			catch (XMLStreamException e) {
				throw new RuntimeException(e);
			}

		});
		writer.setRootTagName("{https://www.springframework.org/test}ns:testroot");
		writer.afterPropertiesSet();
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

		@Override
		public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
			Assert.isInstanceOf(Result.class, result);
			try {
				StaxTestUtils.getXmlEventWriter(result)
					.add(XMLEventFactory.newInstance()
						.createStartElement(namespacePrefix, namespace, graph.toString()));
				StaxTestUtils.getXmlEventWriter(result)
					.add(XMLEventFactory.newInstance().createEndElement(namespacePrefix, namespace, graph.toString()));
			}
			catch (Exception e) {
				throw new RuntimeException("Exception while writing to output file", e);
			}
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

	}

	/**
	 * @return output file content as String
	 */
	private String getOutputFileContent() throws IOException {
		return getOutputFileContent("UTF-8");
	}

	/**
	 * @param encoding the encoding
	 * @return output file content as String
	 */
	private String getOutputFileContent(String encoding) throws IOException {
		return getOutputFileContent(encoding, true);
	}

	/**
	 * @param encoding the encoding
	 * @param discardHeader the flag to strip XML header
	 * @return output file content as String
	 */
	private String getOutputFileContent(String encoding, boolean discardHeader) throws IOException {
		String value = FileUtils.readFileToString(resource.getFile(), encoding);
		if (discardHeader) {
			return value.replaceFirst("<\\?xml.*?\\?>", "");
		}
		return value;
	}

	/**
	 * @return new instance of fully configured writer
	 */
	private StaxEventItemWriter<Object> createItemWriter() throws Exception {
		StaxEventItemWriter<Object> source = new StaxEventItemWriter<>();
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

	@XmlRootElement(name = "item", namespace = "https://www.springframework.org/test")
	private static class JAXBItem {

	}

}
