package org.springframework.batch.io.oxm;

import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.castor.CastorMarshaller;

public class CastorMarshallingTests extends AbstractStaxEventWriterOutputSourceTests {

	protected Marshaller getMarshaller() throws Exception {

		CastorMarshaller marshaller = new CastorMarshaller();
		// marshaller.setTargetClass(Trade.class);
		marshaller.setMappingLocation(new ClassPathResource("mapping-castor.xml", getClass()));
		// there is no way to call
		// org.exolab.castor.xml.Marshaller.setSupressXMLDeclaration();
		marshaller.afterPropertiesSet();
		return marshaller;
	}

}
