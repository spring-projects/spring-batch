/*
 * Copyright 2008-2014 the original author or authors.
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

import java.io.IOException;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.Source;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

import static org.junit.Assert.assertTrue;

public class StaxEventItemReaderCommonTests extends AbstractItemStreamItemReaderTests {

	private final static String FOOS = "<foos> <foo value=\"1\"/> <foo value=\"2\"/> <foo value=\"3\"/> <foo value=\"4\"/> <foo value=\"5\"/> </foos>";

    @Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		StaxEventItemReader<Foo> reader = new StaxEventItemReader<>();
		reader.setResource(new ByteArrayResource(FOOS.getBytes()));
		reader.setFragmentRootElementName("foo");
		reader.setUnmarshaller(new Unmarshaller() {
            @Override
			public Object unmarshal(Source source) throws XmlMappingException, IOException {
				Attribute attr = null ;
				try {
					XMLEventReader eventReader = StaxTestUtils.getXmlEventReader( source);
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

            @Override
			public boolean supports(Class<?> clazz) {
				return true;
			}

		});

		reader.setSaveState(true);
		reader.afterPropertiesSet();
		return reader;
	}

    @Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		StaxEventItemReader<Foo> reader = (StaxEventItemReader<Foo>) tested;
		reader.close();
		
		reader.setResource(new ByteArrayResource("<foos />".getBytes()));
		reader.afterPropertiesSet();
		
		reader.open(new ExecutionContext());
	}

}
