package org.springframework.batch.io.oxm;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.io.oxm.domain.Trade;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.StaxEventItemWriter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;
import org.springframework.util.ClassUtils;

public abstract class AbstractStaxEventWriterItemWriterTests {

	private StaxEventItemWriter<Trade> writer = new StaxEventItemWriter<Trade>();

	private Resource resource;

	File outputFile;

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
		// TODO: commented out while BRITS-214
/*		writer.write(objects);
		writer.close(null);
		XMLUnit.setIgnoreWhitespace(true);
		XMLAssert.assertXMLEqual(new FileReader(expected.getFile()), new FileReader(resource.getFile()));
*/
	}

	@Before
	public void setUp() throws Exception {
		// File outputFile =
		// File.createTempFile("AbstractStaxStreamWriterOutputSourceTests",
		// "xml");
		outputFile = File.createTempFile(ClassUtils.getShortName(this.getClass()), ".xml");
		resource = new FileSystemResource(outputFile);
		writer.setResource(resource);

		writer.setMarshaller(getMarshaller());

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
