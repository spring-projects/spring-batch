package org.springframework.batch.item.xml;

import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class Jaxb2UnmarshallingTests extends AbstractStaxEventReaderItemReaderTests {

	protected Unmarshaller getUnmarshaller() throws Exception {
		reader.setFragmentRootElementName("trade");

		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class<?>[] { Trade.class });
		// marshaller.setSchema(new ClassPathResource("trade.xsd", Trade.class));
		marshaller.afterPropertiesSet();
		
		return marshaller;
	}

}
