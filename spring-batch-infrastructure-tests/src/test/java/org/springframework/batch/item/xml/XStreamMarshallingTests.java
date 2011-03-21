package org.springframework.batch.item.xml;

import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;

import java.util.Collections;

public class XStreamMarshallingTests extends
		AbstractStaxEventWriterItemWriterTests {

	protected Marshaller getMarshaller() throws Exception {
		XStreamMarshaller marshaller = new XStreamMarshaller();
//		marshaller.addAlias("trade", Trade.class);
		marshaller.setAliases(Collections.singletonMap("trade", Trade.class));
		//in XStreamMarshaller.marshalSaxHandlers() method is used SaxWriter, which is configured
		//to include enclosing document (SaxWriter.includeEnclosingDocument is always set to TRUE) 
		return marshaller;
	}

}
