package org.springframework.batch.io.oxm;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.io.oxm.domain.Trade;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.oxm.UnmarshallingEventReaderDeserializer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;

public abstract class AbstractStaxEventReaderItemReaderTests extends TestCase {

	private StaxEventItemReader source = new StaxEventItemReader();

	protected Resource resource = new ClassPathResource(
	"org/springframework/batch/io/oxm/input.xml");


	protected void setUp() throws Exception {
		//TODO sensible resource allocation
		source.setResource(resource);

		source.setFragmentRootElementName("trade");
		UnmarshallingEventReaderDeserializer deserializer = new UnmarshallingEventReaderDeserializer(getUnmarshaller());
		source.setFragmentDeserializer(deserializer);
	
		source.open(new ExecutionContext());

	}

	public void testRead() throws Exception {
		Object result;
		List results = new ArrayList();
		while ((result = source.read()) != null) {
			results.add(result);
		}
		checkResults(results);

	}

	/**
	 * @return Unmarshaller specific to the OXM library used
	 */
	protected abstract Unmarshaller getUnmarshaller() throws Exception;

	/**
	 * @param results list of domain objects returned by input source
	 */
	protected void checkResults(List results){
		assertEquals(3, results.size());

		Trade trade1 = (Trade) results.get(0);
		assertEquals("XYZ0001", trade1.getIsin());
		assertEquals(5, trade1.getQuantity());
		assertEquals(new BigDecimal("11.39"), trade1.getPrice());
		assertEquals("Customer1", trade1.getCustomer());

		Trade trade2 = (Trade) results.get(1);
		assertEquals("XYZ0002", trade2.getIsin());
		assertEquals(2, trade2.getQuantity());
		assertEquals(new BigDecimal("72.99"), trade2.getPrice());
		assertEquals("Customer2", trade2.getCustomer());

		Trade trade3 = (Trade) results.get(2);
		assertEquals("XYZ0003", trade3.getIsin());
		assertEquals(9, trade3.getQuantity());
		assertEquals(new BigDecimal("99.99"), trade3.getPrice());
		assertEquals("Customer3", trade3.getCustomer());
	}

	protected void tearDown() throws Exception {
		source.close(null);
	}

	public void setResource(Resource resource) {
		this.resource = resource;
	}
}
