/*
 * Copyright 2008-2022 the original author or authors.
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

import static org.junit.Assert.assertEquals;
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
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Tests for {@link StaxEventItemWriter}.
 */
public class TransactionalStaxEventItemWriterTests {

	// object under test
	private StaxEventItemWriter<Object> writer;

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	// output file
	private WritableResource resource;

	private ExecutionContext executionContext;

	// test item for writing to output
	private Object item = new Object() {
		@Override
		public String toString() {
			return ClassUtils.getShortName(StaxEventItemWriter.class) + "-testString";
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
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});
		writer.close();
		String content = outputFileContent();
		assertTrue("Wrong content: " + content, content.contains(TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeaderAfterRollback() throws Exception {
		writer.setHeaderCallback(new StaxWriterCallback() {

			@Override
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
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					throw new RuntimeException("Planned");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
		}
		writer.close();
		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});
		writer.close();
		String content = outputFileContent();
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, ("<header/>")));
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeaderAfterFlushAndRollback() throws Exception {
		writer.setHeaderCallback(new StaxWriterCallback() {

			@Override
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
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				try {
					writer.write(items);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				return null;
			}
		});
		writer.update(executionContext);
		writer.close();
		writer.open(executionContext);
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					try {
						writer.write(items);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
					throw new RuntimeException("Planned");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
		}
		writer.close();
		String content = outputFileContent();
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, ("<header/>")));
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, TEST_STRING));
	}

	/**
	 * @return output file content as String
	 */
	private String outputFileContent() throws IOException {
		return FileUtils.readFileToString(resource.getFile(), (String) null);
	}

	/**
	 * Writes object's toString representation as XML comment.
	 */
	private static class SimpleMarshaller implements Marshaller {

		@Override
		public void marshal(Object graph, Result result) throws XmlMappingException, IOException {
			try {
				StaxTestUtils.getXmlEventWriter(result)
						.add(XMLEventFactory.newInstance().createComment(graph.toString()));
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
	 * @return new instance of fully configured writer
	 */
	private StaxEventItemWriter<Object> createItemWriter() throws Exception {
		StaxEventItemWriter<Object> source = new StaxEventItemWriter<>();
		source.setResource(resource);

		Marshaller marshaller = new SimpleMarshaller();
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
