/*
 * Copyright 2010-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.xml;

import static org.junit.Assert.assertTrue;

import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

import org.springframework.batch.item.xml.domain.Trade;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class Jaxb2MarshallingTests extends AbstractStaxEventWriterItemWriterTests {

	@Override
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
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
			transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
			Transformer transformer = transformerFactory.newTransformer();
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
