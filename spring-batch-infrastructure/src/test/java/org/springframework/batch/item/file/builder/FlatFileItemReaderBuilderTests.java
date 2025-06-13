/*
 * Copyright 2016-2023 the original author or authors.
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

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.RecordFieldSetMapper;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.FieldSet;
import org.springframework.batch.item.file.transform.FieldSetFactory;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Drummond Dawson
 * @author Glenn Renfro
 * @author Patrick Baumgartner
 * @author François Martin
 */
class FlatFileItemReaderBuilderTests {

	@Test
	void testSimpleFixedLength() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1  2  3"))
			.fixedLength()
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testSimpleDelimited() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3"))
			.delimited()
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testSimpleDelimitedWithWhitespaceCharacter() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1 2 3"))
			.delimited()
			.delimiter(" ")
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testSimpleDelimitedWithTabCharacter() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1\t2\t3"))
			.delimited()
			.delimiter(DelimitedLineTokenizer.DELIMITER_TAB)
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testAdvancedDelimited() throws Exception {
		final List<String> skippedLines = new ArrayList<>();

		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3\n4,5,$1,2,3$\n@this is a comment\n6,7, 8"))
			.delimited()
			.quoteCharacter('$')
			.names("first", "second", "third")
			.targetType(Foo.class)
			.linesToSkip(1)
			.skippedLinesCallback(skippedLines::add)
			.addComment("@")
			.build();

		ExecutionContext executionContext = new ExecutionContext();
		reader.open(executionContext);

		Foo item = reader.read();
		assertEquals(4, item.getFirst());
		assertEquals(5, item.getSecond());
		assertEquals("1,2,3", item.getThird());

		item = reader.read();
		assertEquals(6, item.getFirst());
		assertEquals(7, item.getSecond());
		assertEquals("8", item.getThird());

		reader.update(executionContext);

		assertNull(reader.read());

		assertEquals("1,2,3", skippedLines.get(0));
		assertEquals(1, skippedLines.size());

		assertEquals(1, executionContext.size());
	}

	@Test
	void testAdvancedFixedLength() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1 2%\n  3\n4 5%\n  6\n@this is a comment\n7 8%\n  9\n"))
			.fixedLength()
			.columns(new Range(1, 2), new Range(3, 5), new Range(6))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.recordSeparatorPolicy(new DefaultRecordSeparatorPolicy("\"", "%"))
			.bufferedReaderFactory((resource,
					encoding) -> new LineNumberReader(new InputStreamReader(resource.getInputStream(), encoding)))
			.maxItemCount(2)
			.saveState(false)
			.build();

		ExecutionContext executionContext = new ExecutionContext();

		reader.open(executionContext);
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());

		item = reader.read();
		assertEquals(4, item.getFirst());
		assertEquals(5, item.getSecond());
		assertEquals("6", item.getThird());

		reader.update(executionContext);

		assertNull(reader.read());
		assertEquals(0, executionContext.size());
	}

	@Test
	void testStrict() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(new FileSystemResource("this/file/does/not/exist"))
			.delimited()
			.names("first", "second", "third")
			.targetType(Foo.class)
			.strict(false)
			.build();

		reader.open(new ExecutionContext());

		assertNull(reader.read());
	}

	@Test
	public void testDelimitedRelaxed() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1 2 3"))
			.delimited()
			.delimiter(" ")
			.strict(false)
			.names("first", "second")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertNull(item.getThird());
	}

	@Test
	public void testDelimitedStrict() {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1 2 3"))
			.delimited()
			.delimiter(" ")
			.strict(true)
			.names("first", "second")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());

		Exception exception = assertThrows(RuntimeException.class, reader::read);
		String expectedMessage = "Parsing error at line: 1 in resource=[Byte array resource [resource loaded from byte array]], input=[1 2 3]";
		String actualMessage = exception.getMessage();
		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	void testCustomLineTokenizerFieldSetMapper() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("|1|&|2|&|  3|\n|4|&|5|&|foo|"))
			.lineTokenizer(line -> new DefaultFieldSet(line.split("&")))
			.fieldSetMapper(fieldSet -> {
				Foo item = new Foo();

				item.setFirst(Integer.parseInt(fieldSet.readString(0).replaceAll("\\|", "")));
				item.setSecond(Integer.parseInt(fieldSet.readString(1).replaceAll("\\|", "")));
				item.setThird(fieldSet.readString(2).replaceAll("\\|", ""));

				return item;
			})
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();

		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("  3", item.getThird());

		item = reader.read();

		assertEquals(4, item.getFirst());
		assertEquals(5, item.getSecond());
		assertEquals("foo", item.getThird());

		assertNull(reader.read());
	}

	@Test
	void testComments() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3\n@this is a comment\n+so is this\n4,5,6"))
			.comments("@", "+")
			.delimited()
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		item = reader.read();
		assertEquals(4, item.getFirst());
		assertEquals(5, item.getSecond());
		assertEquals("6", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testEmptyComments() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3\n4,5,6"))
			.comments(new String[] {})
			.delimited()
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		item = reader.read();
		assertEquals(4, item.getFirst());
		assertEquals(5, item.getSecond());
		assertEquals("6", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testDefaultComments() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3\n4,5,6\n#this is a default comment"))
			.delimited()
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		item = reader.read();
		assertEquals(4, item.getFirst());
		assertEquals(5, item.getSecond());
		assertEquals("6", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testPrototypeBean() throws Exception {
		BeanFactory factory = new AnnotationConfigApplicationContext(Beans.class);

		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3"))
			.delimited()
			.names("first", "second", "third")
			.prototypeBeanName("foo")
			.beanFactory(factory)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testBeanWrapperFieldSetMapperStrict() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3"))
			.delimited()
			.names("setFirst", "setSecond", "setThird")
			.targetType(Foo.class)
			.beanMapperStrict(true)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testDelimitedIncludedFields() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3"))
			.delimited()
			.includedFields(0, 2)
			.addIncludedField(1)
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testDelimitedFieldSetFactory() throws Exception {
		String[] names = { "first", "second", "third" };

		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3"))
			.delimited()
			.fieldSetFactory(new FieldSetFactory() {
				private final FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "3", "foo" }, names);

				@Override
				public FieldSet create(String[] values, String[] names) {
					return fieldSet;
				}

				@Override
				public FieldSet create(String[] values) {
					return fieldSet;
				}
			})
			.names(names)
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(3, item.getSecond());
		assertEquals("foo", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testFixedLengthFieldSetFactory() throws Exception {
		String[] names = { "first", "second", "third" };

		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1  2  3"))
			.fixedLength()
			.fieldSetFactory(new FieldSetFactory() {
				private final FieldSet fieldSet = new DefaultFieldSet(new String[] { "1", "3", "foo" }, names);

				@Override
				public FieldSet create(String[] values, String[] names) {
					return fieldSet;
				}

				@Override
				public FieldSet create(String[] values) {
					return fieldSet;
				}
			})
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(3, item.getSecond());
		assertEquals("foo", item.getThird());
		assertNull(reader.read());
	}

	@Test
	void testName() {
		var builder = new FlatFileItemReaderBuilder<Foo>().resource(getResource("1  2  3"))
			.fixedLength()
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class);
		Exception exception = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("A name is required when saveState is set to true.", exception.getMessage());

		builder = new FlatFileItemReaderBuilder<Foo>().resource(getResource("1  2  3"))
			.fixedLength()
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.name(null);
		exception = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("A name is required when saveState is set to true.", exception.getMessage());

		assertNotNull(new FlatFileItemReaderBuilder<Foo>().resource(getResource("1  2  3"))
			.fixedLength()
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.saveState(false)
			.build(), "builder should return new instance of FlatFileItemReader");

		assertNotNull(new FlatFileItemReaderBuilder<Foo>().resource(getResource("1  2  3"))
			.fixedLength()
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.name("foobar")
			.build(), "builder should return new instance of FlatFileItemReader");

	}

	@Test
	void testDefaultEncoding() {
		String encoding = FlatFileItemReader.DEFAULT_CHARSET;
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1,2,3"))
			.delimited()
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		assertEquals(encoding, ReflectionTestUtils.getField(reader, "encoding"));
	}

	@Test
	void testCustomEncoding() {
		String encoding = "UTF-8";
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1  2  3"))
			.encoding(encoding)
			.fixedLength()
			.columns(new Range(1, 3), new Range(4, 6), new Range(7))
			.names("first", "second", "third")
			.targetType(Foo.class)
			.build();

		assertEquals(encoding, ReflectionTestUtils.getField(reader, "encoding"));
	}

	@Test
	void testErrorMessageWhenNoFieldSetMapperIsProvided() {
		var builder = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1;2;3"))
			.lineTokenizer(line -> new DefaultFieldSet(line.split(";")));
		Exception exception = assertThrows(IllegalStateException.class, builder::build);
		String message = exception.getMessage();
		if (message.equals("No LineTokenizer implementation was provided.")) {
			fail("Error message should not be 'No LineTokenizer implementation was provided.' "
					+ " when a LineTokenizer is provided");
		}
		assertEquals("No FieldSetMapper implementation was provided.", message);
	}

	@Test
	void testErrorMessageWhenNoLineTokenizerWasProvided() {
		try {
			new FlatFileItemReaderBuilder<Foo>().name("fooReader").resource(getResource("1;2;3")).build();
		}
		catch (IllegalStateException exception) {
			String exceptionMessage = exception.getMessage();
			assertEquals("No LineTokenizer implementation was provided.", exceptionMessage);
		}
	}

	@Test
	void testErrorWhenTargetTypeAndFieldSetMapperIsProvided() {
		var builder = new FlatFileItemReaderBuilder<Foo>().name("fooReader")
			.resource(getResource("1;2;3"))
			.lineTokenizer(line -> new DefaultFieldSet(line.split(";")))
			.targetType(Foo.class)
			.fieldSetMapper(fieldSet -> new Foo());
		var exception = assertThrows(IllegalStateException.class, builder::build);
		assertEquals("Either a TargetType or FieldSetMapper can be set, can't be both.", exception.getMessage());
	}

	@Test
	void testSetupWithRecordTargetType() {
		// given
		record Person(int id, String name) {
		}

		// when
		FlatFileItemReader<Person> reader = new FlatFileItemReaderBuilder<Person>().name("personReader")
			.resource(getResource("1,foo"))
			.targetType(Person.class)
			.delimited()
			.names("id", "name")
			.build();

		// then
		Object lineMapper = ReflectionTestUtils.getField(reader, "lineMapper");
		assertNotNull(lineMapper);
		assertTrue(lineMapper instanceof DefaultLineMapper);
		Object fieldSetMapper = ReflectionTestUtils.getField(lineMapper, "fieldSetMapper");
		assertNotNull(fieldSetMapper);
		assertTrue(fieldSetMapper instanceof RecordFieldSetMapper);
	}

	@Test
	void testSetupWithClassTargetType() {
		// given
		class Person {

			int id;

			String name;

		}

		// when
		FlatFileItemReader<Person> reader = new FlatFileItemReaderBuilder<Person>().name("personReader")
			.resource(getResource("1,foo"))
			.targetType(Person.class)
			.delimited()
			.names("id", "name")
			.build();

		// then
		Object lineMapper = ReflectionTestUtils.getField(reader, "lineMapper");
		assertNotNull(lineMapper);
		assertTrue(lineMapper instanceof DefaultLineMapper);
		Object fieldSetMapper = ReflectionTestUtils.getField(lineMapper, "fieldSetMapper");
		assertNotNull(fieldSetMapper);
		assertTrue(fieldSetMapper instanceof BeanWrapperFieldSetMapper);
	}

	private Resource getResource(String contents) {
		return new ByteArrayResource(contents.getBytes());
	}

	public static class Foo {

		private int first;

		private int second;

		private String third;

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

	}

	@Configuration
	static class Beans {

		@Bean
		@Scope("prototype")
		Foo foo() {
			return new Foo();
		}

	}

}
