package org.springframework.batch.item.xml;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

public abstract class AbstractStaxEventWriterItemWriterTests {
	
	private Log logger = LogFactory.getLog(getClass());

	private static final int MAX_WRITE = 100;

	protected StaxEventItemWriter<Trade> writer = new StaxEventItemWriter<Trade>();

	private Resource resource;

	private File outputFile;

	protected Resource expected = new ClassPathResource("expected-output.xml", getClass());

	protected List<Trade> objects = new ArrayList<Trade>() {
		{
			add(new Trade("isin1", 1, new BigDecimal(1.0), "customer1"));
			add(new Trade("isin2", 2, new BigDecimal(2.0), "customer2"));
			add(new Trade("isin3", 3, new BigDecimal(3.0), "customer3"));
		}
	};

	/**
	 * Write list of domain objects and check the output file.
	 */
	@Test
	public void testWrite() throws Exception {
		StopWatch stopWatch = new StopWatch(getClass().getSimpleName());
		stopWatch.start();
		for (int i = 0; i < MAX_WRITE; i++) {
			new TransactionTemplate(new ResourcelessTransactionManager()).execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					try {
						writer.write(objects);
					}
					catch (RuntimeException e) {
						throw e;
					}
					catch (Exception e) {
						throw new IllegalStateException("Exception encountered on write", e);
					}
					return null;
				}
			});
		}
		writer.close();
		stopWatch.stop();
		logger.info("Timing for XML writer: " + stopWatch);
		XMLUnit.setIgnoreWhitespace(true);
		// String content = FileUtils.readFileToString(resource.getFile());
		// System.err.println(content);
		XMLAssert.assertXMLEqual(new FileReader(expected.getFile()), new FileReader(resource.getFile()));

	}

	@Before
	public void setUp() throws Exception {

		File directory = new File("target/data");
		directory.mkdirs();
		outputFile = File.createTempFile(ClassUtils.getShortName(this.getClass()), ".xml", directory);
		resource = new FileSystemResource(outputFile);
		writer.setResource(resource);

		writer.setMarshaller(getMarshaller());
		writer.afterPropertiesSet();

		writer.open(new ExecutionContext());

	}

	@After
	public void tearDown() throws Exception {
		outputFile.delete();
	}

	/**
	 * @return Marshaller specific for the OXM technology being used.
	 */
	protected abstract Marshaller getMarshaller() throws Exception;

}
