package org.springframework.batch.io.oxm;

import org.springframework.batch.io.oxm.domain.Trade;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.xstream.XStreamMarshaller;

public class XStreamMarshallingTests extends
		AbstractStaxEventWriterOutputSourceTests {

	protected Marshaller getMarshaller() throws Exception {
		XStreamMarshaller marshaller = new XStreamMarshaller();
		marshaller.addAlias("trade", Trade.class);
		//in XStreamMarshaller.marshalSaxHandlers() method is used SaxWriter, which is configured
		//to include enclosing document (SaxWriter.includeEnclosingDocument is always set to TRUE) 
		return marshaller;
	}

}
