package org.springframework.batch.item.xml;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;

public class XStreamUnmarshallingTests extends AbstractStaxEventReaderItemReaderTests {

	protected Unmarshaller getUnmarshaller() throws Exception {
		XStreamMarshaller unmarshaller = new XStreamMarshaller();
		Map<String,Class<?>> aliasesMap = new HashMap<String,Class<?>>();
		aliasesMap.put("trade", Trade.class);
		aliasesMap.put("isin", String.class);
		aliasesMap.put("customer", String.class);
		aliasesMap.put("price", BigDecimal.class);
		/*unmarshaller.addAlias("trade", Trade.class);
		unmarshaller.addAlias("isin", String.class);
		unmarshaller.addAlias("customer", String.class);
		unmarshaller.addAlias("price", BigDecimal.class);*/
		unmarshaller.setAliases(aliasesMap);
		return unmarshaller;
	}

}
