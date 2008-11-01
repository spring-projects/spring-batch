package org.springframework.batch.item.xml;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

import org.springframework.batch.item.CommonItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;

public class StaxEventItemReaderCommonTests extends CommonItemStreamItemReaderTests {

	private final String FOOS = "<foos> <foo value=\"1\"/> <foo value=\"2\"/> <foo value=\"3\"/> <foo value=\"4\"/> <foo value=\"5\"/> </foos>";

	protected ItemReader getItemReader() throws Exception {
		StaxEventItemReader reader = new StaxEventItemReader();
		reader.setResource(new ByteArrayResource(FOOS.getBytes()));
		reader.setFragmentRootElementName("foo");
		reader.setFragmentDeserializer(new EventReaderDeserializer() {
			public Object deserializeFragment(XMLEventReader eventReader) {
				Attribute attr;
				try {
					assertTrue(eventReader.nextEvent().isStartDocument());
					StartElement event = eventReader.nextEvent().asStartElement();
					attr = (Attribute) event.getAttributes().next();
				}
				catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}
				Foo foo = new Foo();
				foo.setValue(Integer.parseInt(attr.getValue()));
				return foo;
			}
		});

		reader.setSaveState(true);
		reader.afterPropertiesSet();
		return reader;
	}

	protected void pointToEmptyInput(ItemReader tested) throws Exception {
		StaxEventItemReader reader = (StaxEventItemReader) tested;
		reader.close(new ExecutionContext());
		
		reader.setResource(new ByteArrayResource("<foos />".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
	}

}
