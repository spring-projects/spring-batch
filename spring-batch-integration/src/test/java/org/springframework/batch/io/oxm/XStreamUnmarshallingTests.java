package org.springframework.batch.io.oxm;

import java.math.BigDecimal;

import org.springframework.batch.io.oxm.domain.Trade;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;

public class XStreamUnmarshallingTests extends AbstractStaxEventReaderInputSourceTests {

	protected Unmarshaller getUnmarshaller() throws Exception {
		XStreamMarshaller unmarshaller = new XStreamMarshaller();
		unmarshaller.addAlias("trade", Trade.class);
		unmarshaller.addAlias("isin", String.class);
		unmarshaller.addAlias("customer", String.class);
		unmarshaller.addAlias("price", BigDecimal.class);
		return unmarshaller;
	}

}
