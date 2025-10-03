/*
 * Copyright 2017-2022 the original author or authors.
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
package org.springframework.batch.item.xml.builder;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import jakarta.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLInputFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.xml.StaxEventItemReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.util.ReflectionTestUtils.getField;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Parikshit Dutta
 */
@ExtendWith(MockitoExtension.class)
class StaxEventItemReaderBuilderTests {

	private static final String SIMPLE_XML = "<foos><foo><first>1</first>"
			+ "<second>two</second><third>three</third></foo><foo><first>4</first>"
			+ "<second>five</second><third>six</third></foo><foo><first>7</first>"
			+ "<second>eight</second><third>nine</third></foo></foos>";

	@Mock
	private Resource resource;

	@Test
	void testConfiguration() throws Exception {
		Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setClassesToBeBound(Foo.class);

		StaxEventItemReader<Foo> reader = new StaxEventItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource(SIMPLE_XML))
			.addFragmentRootElements("foo")
			.currentItemCount(1)
			.maxItemCount(2)
			.unmarshaller(unmarshaller)
			.xmlInputFactory(XMLInputFactory.newInstance())
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);
		Foo item = reader.read();
		assertNull(reader.read());
		reader.update(executionContext);

		reader.close();

		assertEquals(4, item.getFirst());
		assertEquals("five", item.getSecond());
		assertEquals("six", item.getThird());
		assertEquals(2, executionContext.size());

		Object executionContextUserSupport = getField(reader, "executionContextUserSupport");
		assertEquals("fooReader", getField(executionContextUserSupport, "name"));
	}

	@Test
	void testCustomEncoding() throws Exception {
		Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setClassesToBeBound(Foo.class);

		Charset charset = StandardCharsets.ISO_8859_1;
		ByteBuffer xml = charset.encode(SIMPLE_XML);

		StaxEventItemReader<Foo> reader = new StaxEventItemReaderBuilder<Foo>().name("fooReader")
			.resource(new ByteArrayResource(xml.array()))
			.encoding(charset.name())
			.addFragmentRootElements("foo")
			.currentItemCount(1)
			.maxItemCount(2)
			.unmarshaller(unmarshaller)
			.xmlInputFactory(XMLInputFactory.newInstance())
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);
		Foo item = reader.read();
		assertNull(reader.read());
		reader.update(executionContext);

		reader.close();

		assertEquals(4, item.getFirst());
		assertEquals("five", item.getSecond());
		assertEquals("six", item.getThird());
		assertEquals(2, executionContext.size());
	}

	@Test
	void testStrict() throws Exception {
		Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setClassesToBeBound(Foo.class);

		StaxEventItemReader<Foo> reader = new StaxEventItemReaderBuilder<Foo>().name("fooReader")
			.resource(this.resource)
			.addFragmentRootElements("foo")
			.unmarshaller(unmarshaller)
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		assertThrows(ItemStreamException.class, () -> reader.open(executionContext));
	}

	@Test
	void testSaveState() throws Exception {
		Jaxb2Marshaller unmarshaller = new Jaxb2Marshaller();
		unmarshaller.setClassesToBeBound(Foo.class);

		StaxEventItemReader<Foo> reader = new StaxEventItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource(SIMPLE_XML))
			.addFragmentRootElements("foo")
			.unmarshaller(unmarshaller)
			.saveState(false)
			.build();

		reader.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);
		Foo item = reader.read();
		item = reader.read();
		item = reader.read();
		assertNull(reader.read());
		reader.update(executionContext);

		reader.close();

		assertEquals(7, item.getFirst());
		assertEquals("eight", item.getSecond());
		assertEquals("nine", item.getThird());
		assertEquals(0, executionContext.size());
	}

	private Resource getResource(String contents) {
		return new ByteArrayResource(contents.getBytes());
	}

	@XmlRootElement(name = "foo")
	public static class Foo {

		private int first;

		private String second;

		private String third;

		public Foo() {
		}

		public Foo(int first, String second, String third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}

		public int getFirst() {
			return first;
		}

		public void setFirst(int first) {
			this.first = first;
		}

		public String getSecond() {
			return second;
		}

		public void setSecond(String second) {
			this.second = second;
		}

		public String getThird() {
			return third;
		}

		public void setThird(String third) {
			this.third = third;
		}

		@Override
		public String toString() {
			return String.format("{%s, %s, %s}", this.first, this.second, this.third);
		}

	}

}
