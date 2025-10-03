/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.batch.item.file;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.Source;

import org.springframework.batch.item.AbstractItemStreamItemReaderTests;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.sample.Foo;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.batch.item.xml.StaxTestUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

class MultiResourceItemReaderXmlTests extends AbstractItemStreamItemReaderTests {

	@Override
	protected ItemReader<Foo> getItemReader() throws Exception {
		Unmarshaller unmarshaller = new Unmarshaller() {
			@Override
			public Object unmarshal(Source source) throws XmlMappingException, IOException {

				Attribute attr;
				try {
					XMLEventReader eventReader = StaxTestUtils.getXmlEventReader(source);
					assertTrue(eventReader.nextEvent().isStartDocument());
					StartElement event = eventReader.nextEvent().asStartElement();
					attr = (Attribute) event.getAttributes().next();
				}
				catch (Exception e) {
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

		};
		StaxEventItemReader<Foo> reader = new StaxEventItemReader<>(unmarshaller);
		reader.setFragmentRootElementName("foo");
		reader.setUnmarshaller(unmarshaller);
		reader.setSaveState(true);

		MultiResourceItemReader<Foo> multiReader = new MultiResourceItemReader<>(reader);
		Resource r1 = new ByteArrayResource("<foos> <foo value=\"1\"/> <foo value=\"2\"/> </foos>".getBytes());
		Resource r2 = new ByteArrayResource("<foos> </foos>".getBytes());
		Resource r3 = new ByteArrayResource("<foos> <foo value=\"3\"/> </foos>".getBytes());
		Resource r4 = new ByteArrayResource("<foos> <foo value=\"4\"/> <foo value=\"5\"/> </foos>".getBytes());

		multiReader.setResources(new Resource[] { r1, r2, r3, r4 });
		multiReader.setSaveState(true);
		multiReader.setComparator((arg0, arg1) -> {
			return 0; // preserve original ordering
		});

		return multiReader;
	}

	@Override
	protected void pointToEmptyInput(ItemReader<Foo> tested) throws Exception {
		MultiResourceItemReader<Foo> multiReader = (MultiResourceItemReader<Foo>) tested;
		multiReader.close();
		multiReader.setResources(new Resource[] { new ByteArrayResource("<foos />".getBytes()) });
		multiReader.open(new ExecutionContext());
	}

}
