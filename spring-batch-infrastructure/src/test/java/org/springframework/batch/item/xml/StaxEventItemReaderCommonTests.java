package org.springframework.batch.item.xml;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.Source;

import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

@RunWith(JUnit4ClassRunner.class)
public class StaxEventItemReaderCommonTests extends AbstractItemStreamItemReaderTests {

	private final static String FOOS = "<foos> <foo value=\"1\"/> <foo value=\"2\"/> <foo value=\"3\"/> <foo value=\"4\"/> <foo value=\"5\"/> </foos>";

	protected ItemReader<Foo> getItemReader() throws Exception {
		StaxEventItemReader<Foo> reader = new StaxEventItemReader<Foo>();
		reader.setResource(new ByteArrayResource(FOOS.getBytes()));
		reader.setFragmentRootElementName("foo");
		reader.setUnmarshaller(new Unmarshaller() {
			public Object unmarshal(Source source) throws XmlMappingException, IOException {
				Attribute attr = null ;
				try {
					XMLEventReader eventReader = StaxUtils.getXmlEventReader( source);
					assertTrue(eventReader.nextEvent().isStartDocument());
					StartElement event = eventReader.nextEvent().asStartElement();
					attr = (Attribute) event.getAttributes().next();
				}
				catch  (Exception e) {
					throw new RuntimeException(e);
				}
				Foo foo = new Foo();
				foo.setValue(Integer.parseInt(attr.getValue()));
				return foo;
			}

			@SuppressWarnings("rawtypes")
			public boolean supports(Class clazz) {
				return true;
			}

		});

		reader.setSaveState(true);
		reader.afterPropertiesSet();
		return reader;
	}

	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		StaxEventItemReader<Foo> reader = (StaxEventItemReader<Foo>) tested;
		reader.close();
		
		reader.setResource(new ByteArrayResource("<foos />".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
	}

}
