package org.springframework.batch.io.oxm;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.springframework.batch.io.file.support.StaxEventWriterItemWriter;
import org.springframework.batch.io.file.support.oxm.MarshallingObjectToXmlSerializer;
import org.springframework.batch.io.oxm.domain.Trade;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Marshaller;

public abstract class AbstractStaxEventWriterItemWriterTests extends TestCase {

	private StaxEventWriterItemWriter source = new StaxEventWriterItemWriter();

	private Resource resource;

	File outputFile;

	protected Resource expected = new ClassPathResource("expected-output.xml", getClass());


	protected List objects = new ArrayList() {{
		add(new Trade("isin1", 1, new BigDecimal(1.0), "customer1"));
		add(new Trade("isin2", 2, new BigDecimal(2.0), "customer2"));
		add(new Trade("isin3", 3, new BigDecimal(3.0), "customer3"));
	}};

	/**
	 * Write list of domain objects and check the output file.
	 */
	public void testWrite() throws Exception {
		for (Iterator iterator = objects.listIterator(); iterator.hasNext();) {
			source.write(iterator.next());
		}
		source.close();
		XMLUnit.setIgnoreWhitespace(true);
		XMLAssert.assertXMLEqual(new FileReader(expected.getFile()), new FileReader(resource.getFile()));

	}


	protected void setUp() throws Exception {
		//File outputFile = File.createTempFile("AbstractStaxStreamWriterOutputSourceTests", "xml");
		outputFile = File.createTempFile(this.getClass().getSimpleName(), ".xml");
		resource = new FileSystemResource(outputFile);
		source.setResource(resource);

		MarshallingObjectToXmlSerializer mapper = new MarshallingObjectToXmlSerializer(getMarshaller());
		source.setSerializer(mapper);
	}

	protected void tearDown() throws Exception {
		super.tearDown();

		outputFile.delete();
	}

	/**
	 * @return Marshaller specific for the OXM technology being used.
	 */
	protected abstract Marshaller getMarshaller() throws Exception;

}
