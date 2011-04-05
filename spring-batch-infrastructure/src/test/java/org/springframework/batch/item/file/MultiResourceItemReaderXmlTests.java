package org.springframework.batch.item.file;

import java.io.IOException;
import java.util.Comparator;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.Source;

import junit.framework.Assert;

import org.junit.internal.runners.JUnit4ClassRunner;
import org.junit.runner.RunWith;
import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

@RunWith(JUnit4ClassRunner.class)
public class MultiResourceItemReaderXmlTests extends AbstractItemStreamItemReaderTests {

	protected ItemReader<Foo> getItemReader() throws Exception {
		MultiResourceItemReader<Foo> multiReader = new MultiResourceItemReader<Foo>();

		StaxEventItemReader<Foo> reader = new StaxEventItemReader<Foo>();

		reader.setFragmentRootElementName("foo");
		reader.setUnmarshaller(new Unmarshaller() {
			public Object unmarshal(Source source) throws XmlMappingException, IOException {


				Attribute attr;
				try {
					XMLEventReader eventReader = StaxUtils.getXmlEventReader(source );
					Assert.assertTrue(eventReader.nextEvent().isStartDocument());
					StartElement event = eventReader.nextEvent().asStartElement();
					attr = (Attribute) event.getAttributes().next();
				}
				catch ( Exception e) {
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

		Resource r1 = new ByteArrayResource("<foos> <foo value=\"1\"/> <foo value=\"2\"/> </foos>".getBytes());
		Resource r2 = new ByteArrayResource("<foos> </foos>".getBytes());
		Resource r3 = new ByteArrayResource("<foos> <foo value=\"3\"/> </foos>".getBytes());
		Resource r4 = new ByteArrayResource("<foos> <foo value=\"4\"/> <foo value=\"5\"/> </foos>".getBytes());

		multiReader.setDelegate(reader);
		multiReader.setResources(new Resource[] { r1, r2, r3, r4 });
		multiReader.setSaveState(true);
		multiReader.setComparator(new Comparator<Resource>() {
			public int compare(Resource arg0, Resource arg1) {
				return 0; // preserve original ordering
			}
		});

		return multiReader;
	}
	
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		MultiResourceItemReader<Foo> multiReader = (MultiResourceItemReader<Foo>) tested;
		multiReader.close();
		multiReader.setResources(new Resource[] { new ByteArrayResource("<foos />"
				.getBytes()) });
		multiReader.open(new ExecutionContext());
	}

}
