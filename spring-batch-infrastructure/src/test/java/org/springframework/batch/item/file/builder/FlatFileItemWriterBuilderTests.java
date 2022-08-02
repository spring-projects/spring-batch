/*
 * Copyright 2016-2022 the original author or authors.
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
package org.springframework.batch.item.file.builder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.batch.item.file.transform.FormatterLineAggregator;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.batch.item.file.transform.RecordFieldExtractor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @author Glenn Renfro
 */
class FlatFileItemWriterBuilderTests {

	// reads the output file to check the result
	private BufferedReader reader;

	@Test
	void testMissingLineAggregator() {
		FlatFileItemWriterBuilder<Foo> builder = new FlatFileItemWriterBuilder<>();
		assertThrows(IllegalArgumentException.class, builder::build);
	}

	@Test
	void testMultipleLineAggregators() throws IOException {
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriterBuilder<Foo> builder = new FlatFileItemWriterBuilder<Foo>().name("itemWriter")
				.resource(output).delimited().delimiter(";").names("foo", "bar").formatted().format("%2s%2s")
				.names("foo", "bar");
		assertThrows(IllegalStateException.class, builder::build);
	}

	@Test
	void test() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").lineAggregator(new PassThroughLineAggregator<>()).encoding("UTF-16LE")
				.headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$Foo{first=1, second=2, third='3'}$Foo{first=4, second=5, third='6'}$FOOTER",
				readLine("UTF-16LE", output));
	}

	@Test
	void testDelimitedOutputWithDefaultDelimiter() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").delimited().names("first", "second", "third").encoding("UTF-16LE")
				.headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$1,2,3$4,5,6$FOOTER", readLine("UTF-16LE", output));
	}

	@Test
	void testDelimitedOutputWithEmptyDelimiter() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").delimited().delimiter("").names("first", "second", "third").encoding("UTF-16LE")
				.headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$123$456$FOOTER", readLine("UTF-16LE", output));
	}

	@Test
	void testDelimitedOutputWithDefaultFieldExtractor() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").delimited().delimiter(";").names("first", "second", "third").encoding("UTF-16LE")
				.headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$1;2;3$4;5;6$FOOTER", readLine("UTF-16LE", output));
	}

	@Test
	void testDelimitedOutputWithCustomFieldExtractor() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").delimited().delimiter(" ")
				.fieldExtractor(item -> new Object[] { item.getFirst(), item.getThird() }).encoding("UTF-16LE")
				.headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$1 3$4 6$FOOTER", readLine("UTF-16LE", output));
	}

	@Test
	void testFormattedOutputWithDefaultFieldExtractor() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").formatted().format("%2s%2s%2s").names("first", "second", "third")
				.encoding("UTF-16LE").headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$ 1 2 3$ 4 5 6$FOOTER", readLine("UTF-16LE", output));
	}

	@Test
	void testFormattedOutputWithCustomFieldExtractor() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.lineSeparator("$").formatted().format("%3s%3s")
				.fieldExtractor(item -> new Object[] { item.getFirst(), item.getThird() }).encoding("UTF-16LE")
				.headerCallback(writer1 -> writer1.append("HEADER"))
				.footerCallback(writer12 -> writer12.append("FOOTER")).build();

		ExecutionContext executionContext = new ExecutionContext();

		writer.open(executionContext);

		writer.write(Arrays.asList(new Foo(1, 2, "3"), new Foo(4, 5, "6")));

		writer.close();

		assertEquals("HEADER$  1  3$  4  6$FOOTER", readLine("UTF-16LE", output));
	}

	@Test
	void testFlags() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		String encoding = Charset.defaultCharset().name();

		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").resource(output)
				.shouldDeleteIfEmpty(true).shouldDeleteIfExists(false).saveState(false).forceSync(true).append(true)
				.transactional(false).lineAggregator(new PassThroughLineAggregator<>()).build();

		validateBuilderFlags(writer, encoding);
	}

	@Test
	void testFlagsWithEncoding() throws Exception {

		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));
		String encoding = "UTF-8";
		FlatFileItemWriter<Foo> writer = new FlatFileItemWriterBuilder<Foo>().name("foo").encoding(encoding)
				.resource(output).shouldDeleteIfEmpty(true).shouldDeleteIfExists(false).saveState(false).forceSync(true)
				.append(true).transactional(false).lineAggregator(new PassThroughLineAggregator<>()).build();
		validateBuilderFlags(writer, encoding);
	}

	@Test
	void testSetupDelimitedLineAggregatorWithRecordItemType() throws IOException {
		// given
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));
		record Person(int id, String name) {
		}

		// when
		FlatFileItemWriter<Person> writer = new FlatFileItemWriterBuilder<Person>().name("personWriter")
				.resource(output).delimited().sourceType(Person.class).names("id", "name").build();

		// then
		Object lineAggregator = ReflectionTestUtils.getField(writer, "lineAggregator");
		assertNotNull(lineAggregator);
		assertTrue(lineAggregator instanceof DelimitedLineAggregator);
		Object fieldExtractor = ReflectionTestUtils.getField(lineAggregator, "fieldExtractor");
		assertNotNull(fieldExtractor);
		assertTrue(fieldExtractor instanceof RecordFieldExtractor);
	}

	@Test
	void testSetupDelimitedLineAggregatorWithClassItemType() throws IOException {
		// given
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));
		class Person {

			int id;

			String name;

		}

		// when
		FlatFileItemWriter<Person> writer = new FlatFileItemWriterBuilder<Person>().name("personWriter")
				.resource(output).delimited().sourceType(Person.class).names("id", "name").build();

		// then
		Object lineAggregator = ReflectionTestUtils.getField(writer, "lineAggregator");
		assertNotNull(lineAggregator);
		assertTrue(lineAggregator instanceof DelimitedLineAggregator);
		Object fieldExtractor = ReflectionTestUtils.getField(lineAggregator, "fieldExtractor");
		assertNotNull(fieldExtractor);
		assertTrue(fieldExtractor instanceof BeanWrapperFieldExtractor);
	}

	@Test
	void testSetupDelimitedLineAggregatorWithNoItemType() throws IOException {
		// given
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		// when
		FlatFileItemWriter writer = new FlatFileItemWriterBuilder<>().name("personWriter").resource(output).delimited()
				.names("id", "name").build();

		// then
		Object lineAggregator = ReflectionTestUtils.getField(writer, "lineAggregator");
		assertNotNull(lineAggregator);
		assertTrue(lineAggregator instanceof DelimitedLineAggregator);
		Object fieldExtractor = ReflectionTestUtils.getField(lineAggregator, "fieldExtractor");
		assertNotNull(fieldExtractor);
		assertTrue(fieldExtractor instanceof BeanWrapperFieldExtractor);
	}

	@Test
	void testSetupFormatterLineAggregatorWithRecordItemType() throws IOException {
		// given
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));
		record Person(int id, String name) {
		}

		// when
		FlatFileItemWriter<Person> writer = new FlatFileItemWriterBuilder<Person>().name("personWriter")
				.resource(output).formatted().format("%2s%2s").sourceType(Person.class).names("id", "name").build();

		// then
		Object lineAggregator = ReflectionTestUtils.getField(writer, "lineAggregator");
		assertNotNull(lineAggregator);
		assertTrue(lineAggregator instanceof FormatterLineAggregator);
		Object fieldExtractor = ReflectionTestUtils.getField(lineAggregator, "fieldExtractor");
		assertNotNull(fieldExtractor);
		assertTrue(fieldExtractor instanceof RecordFieldExtractor);
	}

	@Test
	void testSetupFormatterLineAggregatorWithClassItemType() throws IOException {
		// given
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));
		class Person {

			int id;

			String name;

		}

		// when
		FlatFileItemWriter<Person> writer = new FlatFileItemWriterBuilder<Person>().name("personWriter")
				.resource(output).formatted().format("%2s%2s").sourceType(Person.class).names("id", "name").build();

		// then
		Object lineAggregator = ReflectionTestUtils.getField(writer, "lineAggregator");
		assertNotNull(lineAggregator);
		assertTrue(lineAggregator instanceof FormatterLineAggregator);
		Object fieldExtractor = ReflectionTestUtils.getField(lineAggregator, "fieldExtractor");
		assertNotNull(fieldExtractor);
		assertTrue(fieldExtractor instanceof BeanWrapperFieldExtractor);
	}

	@Test
	void testSetupFormatterLineAggregatorWithNoItemType() throws IOException {
		// given
		WritableResource output = new FileSystemResource(File.createTempFile("foo", "txt"));

		// when
		FlatFileItemWriter writer = new FlatFileItemWriterBuilder<>().name("personWriter").resource(output).formatted()
				.format("%2s%2s").names("id", "name").build();

		// then
		Object lineAggregator = ReflectionTestUtils.getField(writer, "lineAggregator");
		assertNotNull(lineAggregator);
		assertTrue(lineAggregator instanceof FormatterLineAggregator);
		Object fieldExtractor = ReflectionTestUtils.getField(lineAggregator, "fieldExtractor");
		assertNotNull(fieldExtractor);
		assertTrue(fieldExtractor instanceof BeanWrapperFieldExtractor);
	}

	private void validateBuilderFlags(FlatFileItemWriter<Foo> writer, String encoding) {
		assertFalse((Boolean) ReflectionTestUtils.getField(writer, "saveState"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "append"));
		assertFalse((Boolean) ReflectionTestUtils.getField(writer, "transactional"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "shouldDeleteIfEmpty"));
		assertFalse((Boolean) ReflectionTestUtils.getField(writer, "shouldDeleteIfExists"));
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "forceSync"));
		assertEquals(encoding, ReflectionTestUtils.getField(writer, "encoding"));
	}

	private String readLine(String encoding, Resource outputFile) throws IOException {

		if (reader == null) {
			reader = new BufferedReader(new InputStreamReader(outputFile.getInputStream(), encoding));
		}

		return reader.readLine();
	}

	public static class Foo {

		private int first;

		private int second;

		private String third;

		public Foo(int first, int second, String third) {
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

		public int getSecond() {
			return second;
		}

		public void setSecond(int second) {
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
			return "Foo{" + "first=" + first + ", second=" + second + ", third='" + third + '\'' + '}';
		}

	}

}
