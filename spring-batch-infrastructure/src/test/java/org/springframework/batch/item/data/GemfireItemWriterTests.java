/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.batch.item.data;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.SpELItemKeyMapper;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.core.convert.converter.Converter;

@ExtendWith(MockitoExtension.class)
class GemfireItemWriterTests {

	private GemfireItemWriter<String, Foo> writer;

	@Mock
	private GemfireTemplate template;

	@BeforeEach
	void setUp() throws Exception {
		writer = new GemfireItemWriter<>();
		writer.setTemplate(template);
		writer.setItemKeyMapper(new SpELItemKeyMapper<>("bar.val"));
		writer.afterPropertiesSet();
	}

	@Test
	void testAfterPropertiesSet() throws Exception {
		writer = new GemfireItemWriter<>();
		assertThrows(IllegalArgumentException.class, writer::afterPropertiesSet);

		writer.setTemplate(template);
		assertThrows(IllegalArgumentException.class, writer::afterPropertiesSet);

		writer.setItemKeyMapper(new SpELItemKeyMapper<>("foo"));
		writer.afterPropertiesSet();
	}

	@Test
	void testBasicWrite() throws Exception {
		Chunk<Foo> chunk = new Chunk<Foo>() {
			{
				add(new Foo(new Bar("val1")));
				add(new Foo(new Bar("val2")));
			}
		};

		writer.write(chunk);

		List<Foo> items = chunk.getItems();
		verify(template).put("val1", items.get(0));
		verify(template).put("val2", items.get(1));
	}

	@Test
	void testBasicDelete() throws Exception {
		Chunk<Foo> chunk = new Chunk<Foo>() {
			{
				add(new Foo(new Bar("val1")));
				add(new Foo(new Bar("val2")));
			}
		};
		writer.setDelete(true);
		writer.write(chunk);

		verify(template).remove("val1");
		verify(template).remove("val2");
	}

	@Test
	void testWriteWithCustomItemKeyMapper() throws Exception {
		Chunk<Foo> chunk = new Chunk<Foo>() {
			{
				add(new Foo(new Bar("val1")));
				add(new Foo(new Bar("val2")));
			}
		};
		writer = new GemfireItemWriter<>();
		writer.setTemplate(template);
		writer.setItemKeyMapper(new Converter<Foo, String>() {

			@Override
			public String convert(Foo item) {
				String index = item.bar.val.replaceAll("val", "");
				return "item" + index;
			}
		});
		writer.afterPropertiesSet();
		writer.write(chunk);

		List<Foo> items = chunk.getItems();
		verify(template).put("item1", items.get(0));
		verify(template).put("item2", items.get(1));
	}

	@Test
	void testWriteNoTransactionNoItems() throws Exception {
		writer.write(null);
		verifyNoInteractions(template);
	}

	static class Foo {

		public Bar bar;

		public Foo(Bar bar) {
			this.bar = bar;
		}

	}

	static class Bar {

		public String val;

		public Bar(String b1) {
			this.val = b1;
		}

	}

}
