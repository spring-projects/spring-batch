package org.springframework.batch.io.oxm;

import org.springframework.core.io.ClassPathResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.castor.CastorMarshaller;

public class CastorUnmarshallingTests extends AbstractStaxEventReaderItemReaderTests {

	protected Unmarshaller getUnmarshaller() throws Exception {
		CastorMarshaller unmarshaller = new CastorMarshaller();
		unmarshaller.setMappingLocation(new ClassPathResource("mapping-castor.xml", getClass()));
		// alternatively target class can be set
		//unmarshaller.setTargetClass(Trade.class);
		unmarshaller.afterPropertiesSet();
		return unmarshaller;
	}

}
