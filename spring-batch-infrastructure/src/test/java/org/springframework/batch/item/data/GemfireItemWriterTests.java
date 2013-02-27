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

@SuppressWarnings({ "rawtypes", "serial", "unchecked" })
public class GemfireItemWriterTests {

	private GemfireItemWriter writer;
	@Mock
	private GemfireTemplate template;

	//private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		writer = new GemfireItemWriter();
		writer.setTemplate(template);
		writer.setItemKeyMapper(new SpELItemKeyMapper<String, Foo>("bar.val"));
		writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new GemfireItemWriter();

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

		writer.setItemKeyMapper(new SpELItemKeyMapper<Object, Object>("foo"));
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
		writer = new GemfireItemWriter();
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
