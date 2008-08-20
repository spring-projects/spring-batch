package org.springframework.batch.item.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Result;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.oxm.MarshallingEventWriterSerializer;
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
import org.springframework.xml.transform.StaxResult;

/**
 * Tests for {@link StaxEventItemWriter}.
 */
public class TransactionalStaxEventItemWriterTests {

	// object under test
	private StaxEventItemWriter<Object> writer;

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

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
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				writer.write(items);
				return null;
			}
		});
		writer.close(executionContext);
		String content = outputFileContent();
		assertTrue("Wrong content: " + content, content.contains(TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeaderAfterRollback() throws Exception {
		Object header = new Object();
		writer.setHeaderItems(new Object[] { header });
		writer.open(executionContext);
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					writer.write(items);
					throw new RuntimeException("Planned");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
		}
		writer.close(executionContext);
		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				writer.write(items);
				return null;
			}
		});
		writer.close(executionContext);
		String content = outputFileContent();
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, ("<!--" + header + "-->")));
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, TEST_STRING));
	}

	/**
	 * Item is written to the output file only after flush.
	 */
	@Test
	public void testWriteWithHeaderAfterFlushAndRollback() throws Exception {
		Object header = new Object();
		writer.setHeaderItems(new Object[] { header });
		writer.open(executionContext);
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				writer.write(items);
				return null;
			}
		});
		writer.update(executionContext);
		writer.close(executionContext);
		writer.open(executionContext);
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					writer.write(items);
					throw new RuntimeException("Planned");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			// expected
		}
		writer.close(executionContext);
		String content = outputFileContent();
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, ("<!--" + header + "-->")));
		assertEquals("Wrong content: " + content, 1, StringUtils.countOccurrencesOf(content, TEST_STRING));
	}

	/**
	 * @return output file content as String
	 */
	private String outputFileContent() throws IOException {
		return FileUtils.readFileToString(resource.getFile(), null);
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
