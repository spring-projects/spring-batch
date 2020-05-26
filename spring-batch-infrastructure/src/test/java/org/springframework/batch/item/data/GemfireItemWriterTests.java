/*
 * Copyright 2013-2014 the original author or authors.
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

import static org.junit.Assert.fail;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.item.SpELItemKeyMapper;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.core.convert.converter.Converter;

@SuppressWarnings("serial")
public class GemfireItemWriterTests {

	private GemfireItemWriter<String, Foo> writer;
	@Mock
	private GemfireTemplate template;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		writer = new GemfireItemWriter<>();
		writer.setTemplate(template);
		writer.setItemKeyMapper(new SpELItemKeyMapper<>("bar.val"));
		writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new GemfireItemWriter<>();

		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		} catch (IllegalArgumentException iae) {
		}

		writer.setTemplate(template);
		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		} catch (IllegalArgumentException iae) {
		}

		writer.setItemKeyMapper(new SpELItemKeyMapper<>("foo"));
		writer.afterPropertiesSet();
	}

	@Test
	public void testBasicWrite() throws Exception {
		List<Foo> items = new ArrayList<Foo>() {
			{
				add(new Foo(new Bar("val1")));
				add(new Foo(new Bar("val2")));
			}
		};

		writer.write(items);

		verify(template).put("val1", items.get(0));
		verify(template).put("val2", items.get(1));
	}

	@Test
	public void testBasicDelete() throws Exception {
		List<Foo> items = new ArrayList<Foo>() {
			{
				add(new Foo(new Bar("val1")));
				add(new Foo(new Bar("val2")));
			}
		};
		writer.setDelete(true);
		writer.write(items);

		verify(template).remove("val1");
		verify(template).remove("val2");
	}

	@Test
	public void testWriteWithCustomItemKeyMapper() throws Exception {
		List<Foo> items = new ArrayList<Foo>() {
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
		writer.write(items);

		verify(template).put("item1", items.get(0));
		verify(template).put("item2", items.get(1));
	}

	@Test
	public void testWriteNoTransactionNoItems() throws Exception {
		writer.write(null);
		verifyZeroInteractions(template);
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
