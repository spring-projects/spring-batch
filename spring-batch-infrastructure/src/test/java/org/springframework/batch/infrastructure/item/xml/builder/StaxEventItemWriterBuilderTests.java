/*
 * Copyright 2017-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.xml.builder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamException;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.xml.StaxEventItemWriter;
import org.springframework.batch.infrastructure.item.xml.builder.StaxEventItemWriterBuilder;
import org.springframework.batch.infrastructure.support.transaction.TransactionAwareBufferedWriter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.WritableResource;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Michael Minella
 * @author Parikshit Dutta
 * @author Mahmoud Ben Hassine
 */
class StaxEventItemWriterBuilderTests {

	private WritableResource resource;

	private Chunk<Foo> items;

	private Marshaller marshaller;

	private static final String FULL_OUTPUT = "<?xml version='1.1' encoding='UTF-16'?>"
			+ "<foobarred baz=\"quix\">\uFEFF<ns:group><ns2:item xmlns:ns2=\"https://www.springframework.org/test\">"
			+ "<first>1</first><second>two</second><third>three</third></ns2:item>\uFEFF"
			+ "<ns2:item xmlns:ns2=\"https://www.springframework.org/test\"><first>4</first>"
			+ "<second>five</second><third>six</third></ns2:item>\uFEFF"
			+ "<ns2:item xmlns:ns2=\"https://www.springframework.org/test\"><first>7</first>"
			+ "<second>eight</second><third>nine</third></ns2:item>\uFEFF</ns:group>\uFEFF" + "</foobarred>";

	@BeforeEach
	void setUp() throws IOException {
		File directory = new File("target/data");
		directory.mkdirs();
		this.resource = new FileSystemResource(
				File.createTempFile("StaxEventItemWriterBuilderTests", ".xml", directory));

		this.items = new Chunk<>();
		this.items.add(new Foo(1, "two", "three"));
		this.items.add(new Foo(4, "five", "six"));
		this.items.add(new Foo(7, "eight", "nine"));

		marshaller = new Jaxb2Marshaller();
		((Jaxb2Marshaller) marshaller).setClassesToBeBound(Foo.class);
	}

	@Test
	void testOverwriteOutput() throws Exception {
		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.marshaller(marshaller)
			.resource(this.resource)
			.overwriteOutput(false)
			.build();
		staxEventItemWriter.afterPropertiesSet();
		assertThrows(ItemStreamException.class, () -> staxEventItemWriter.open(new ExecutionContext()));
	}

	@Test
	void testDeleteIfEmpty() throws Exception {
		ExecutionContext executionContext = new ExecutionContext();

		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.resource(this.resource)
			.marshaller(this.marshaller)
			.shouldDeleteIfEmpty(true)
			.build();

		staxEventItemWriter.afterPropertiesSet();
		staxEventItemWriter.open(executionContext);
		staxEventItemWriter.write(new Chunk<>());
		staxEventItemWriter.update(executionContext);
		staxEventItemWriter.close();

		File file = this.resource.getFile();

		assertFalse(file.exists());
	}

	@Test
	void testTransactional() {

		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.resource(this.resource)
			.marshaller(this.marshaller)
			.transactional(true)
			.forceSync(true)
			.build();

		ExecutionContext executionContext = new ExecutionContext();

		staxEventItemWriter.open(executionContext);

		Object writer = ReflectionTestUtils.getField(staxEventItemWriter, "bufferedWriter");

		assertTrue(writer instanceof TransactionAwareBufferedWriter);

		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "forceSync"));
	}

	@Test
	void testConfiguration() throws Exception {
		Map<String, String> rootElementAttributes = new HashMap<>();
		rootElementAttributes.put("baz", "quix");

		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.marshaller(marshaller)
			.encoding("UTF-16")
			.footerCallback(writer -> {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				try {
					writer.add(factory.createEndElement("ns", "https://www.springframework.org/test", "group"));
				}
				catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}
			})
			.headerCallback(writer -> {
				XMLEventFactory factory = XMLEventFactory.newInstance();
				try {
					writer.add(factory.createStartElement("ns", "https://www.springframework.org/test", "group"));
				}
				catch (XMLStreamException e) {
					throw new RuntimeException(e);
				}
			})
			.resource(this.resource)
			.rootTagName("foobarred")
			.rootElementAttributes(rootElementAttributes)
			.saveState(false)
			.version("1.1")
			.build();

		staxEventItemWriter.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		staxEventItemWriter.open(executionContext);

		staxEventItemWriter.write(this.items);

		staxEventItemWriter.update(executionContext);
		staxEventItemWriter.close();

		assertEquals(FULL_OUTPUT, getOutputFileContent("UTF-16"));
		assertEquals(0, executionContext.size());
	}

	@Test
	void testMissingMarshallerValidation() {
		var builder = new StaxEventItemWriterBuilder<Foo>().name("fooWriter");
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testMissingNameValidation() {
		var builder = new StaxEventItemWriterBuilder<Foo>().marshaller(new Jaxb2Marshaller());
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testStandaloneDeclarationInHeaderWhenNotSet() throws Exception {
		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.marshaller(marshaller)
			.resource(this.resource)
			.build();

		staxEventItemWriter.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		staxEventItemWriter.open(executionContext);
		staxEventItemWriter.write(this.items);
		staxEventItemWriter.close();

		String output = getOutputFileContent(staxEventItemWriter.getEncoding());
		assertFalse(output.contains("standalone="));
	}

	@Test
	void testStandaloneDeclarationInHeaderWhenSetToTrue() throws Exception {
		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.marshaller(marshaller)
			.resource(this.resource)
			.standalone(true)
			.build();

		staxEventItemWriter.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		staxEventItemWriter.open(executionContext);
		staxEventItemWriter.write(this.items);
		staxEventItemWriter.close();

		String output = getOutputFileContent(staxEventItemWriter.getEncoding());
		assertTrue(output.contains("standalone='yes'"));
	}

	@Test
	void testStandaloneDeclarationInHeaderWhenSetToFalse() throws Exception {
		StaxEventItemWriter<Foo> staxEventItemWriter = new StaxEventItemWriterBuilder<Foo>().name("fooWriter")
			.marshaller(marshaller)
			.resource(this.resource)
			.standalone(false)
			.build();

		staxEventItemWriter.afterPropertiesSet();

		ExecutionContext executionContext = new ExecutionContext();
		staxEventItemWriter.open(executionContext);
		staxEventItemWriter.write(this.items);
		staxEventItemWriter.close();

		String output = getOutputFileContent(staxEventItemWriter.getEncoding());
		assertTrue(output.contains("standalone='no'"));
	}

	/**
	 * @param encoding the encoding
	 * @return output file content as String
	 */
	private String getOutputFileContent(String encoding) throws IOException {
		return FileUtils.readFileToString(resource.getFile(), encoding);
	}

	@XmlRootElement(name = "item", namespace = "https://www.springframework.org/test")
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

	}

}
