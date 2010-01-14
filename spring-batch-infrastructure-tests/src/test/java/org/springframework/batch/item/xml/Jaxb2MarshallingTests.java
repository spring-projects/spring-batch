package org.springframework.batch.item.xml;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class Jaxb2MarshallingTests extends AbstractStaxEventWriterItemWriterTests {

	protected Marshaller getMarshaller() throws Exception {
		
		Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
		marshaller.setClassesToBeBound(new Class<?>[] { Trade.class });
		marshaller.afterPropertiesSet();
		
		StringWriter string = new StringWriter();
		marshaller.marshal(new Trade("FOO", 100, BigDecimal.valueOf(10.), "bar"), new StreamResult(string));
		String content = string.toString();
		assertTrue("Wrong content: "+content, content.contains("<customer>bar</customer>"));
		return marshaller;
	}

	public static String getTextFromSource(Source source) {
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			StreamResult stream = new StreamResult(new StringWriter());
			transformer.transform(source, stream);
			return stream.getWriter().toString();
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
