/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.file.builder;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.separator.DefaultRecordSeparatorPolicy;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.Range;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Michael Minella
 */
public class FlatFileItemReaderBuilderTests {

	@Test
	public void testSimpleFixedLength() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
				.name("fooReader")
				.resource(getResource("1  2  3"))
				.fixedLength()
				.ranges(new Range[] {new Range(1, 3), new Range(4, 6), new Range(7)})
				.names(new String[] {"first", "second", "third"})
				.beanMapperClass(Foo.class)
				.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	public void testSimpleDelimited() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
				.name("fooReader")
				.resource(getResource("1,2,3"))
				.delimited()
				.names(new String[] {"first", "second", "third"})
				.beanMapperClass(Foo.class)
				.build();

		reader.open(new ExecutionContext());
		Foo item = reader.read();
		assertEquals(1, item.getFirst());
		assertEquals(2, item.getSecond());
		assertEquals("3", item.getThird());
		assertNull(reader.read());
	}

	@Test
	public void testAdvancedDelimited() throws Exception {
		final List<String> skippedLines = new ArrayList<>();

		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
				.name("fooReader")
				.resource(getResource("1,2,3\n4,5,$1,2,3$\n@this is a comment\n6,7, 8"))
				.delimited()
				.quoteCharacter('$')
				.names(new String[] {"first", "second", "third"})
				.beanMapperClass(Foo.class)
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
	public void testAdvancedFixedLength() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
				.name("fooReader")
				.resource(getResource("1 2%\n  3\n4 5%\n  6\n@this is a comment\n7 8%\n  9\n"))
				.fixedLength()
				.ranges(new Range[] {new Range(1, 2), new Range(3, 5), new Range(6)})
				.names(new String[] {"first", "second", "third"})
				.beanMapperClass(Foo.class)
				.recordSeparatorPolicy(new DefaultRecordSeparatorPolicy("\"", "%"))
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
	public void testStrict() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
				.name("fooReader")
				.resource(new FileSystemResource("this/file/does/not/exist"))
				.delimited()
				.names(new String[] {"first", "second", "third"})
				.beanMapperClass(Foo.class)
				.strict(false)
				.build();

		reader.open(new ExecutionContext());

		assertNull(reader.read());
	}

	@Test
	public void testCustomLineTokenizerFieldSetMapper() throws Exception {
		FlatFileItemReader<Foo> reader = new FlatFileItemReaderBuilder<Foo>()
				.name("fooReader")
				.resource(getResource("|1|&|2|&|  3|\n|4|&|5|&|foo|"))
				.lineTokenizer(line -> new DefaultFieldSet(line.split("&")))
				.fieldSetMapper(fieldSet -> {
					Foo item = new Foo();

					item.setFirst(Integer.valueOf(fieldSet.readString(0).replaceAll("\\|", "")));
					item.setSecond(Integer.valueOf(fieldSet.readString(1).replaceAll("\\|", "")));
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

}
