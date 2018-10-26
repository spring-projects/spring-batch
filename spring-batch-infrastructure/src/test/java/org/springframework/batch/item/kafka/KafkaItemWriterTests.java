package org.springframework.batch.item.kafka;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class KafkaItemWriterTests {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private KafkaItemKeyMapper itemKeyMapper;

	private KafkaItemWriter<String, String> writer;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		when(kafkaTemplate.getDefaultTopic()).thenReturn("defaultTopic");
		itemKeyMapper = new KafkaItemKeyMapper();
		writer = new KafkaItemWriter<>();
		writer.setKafkaTemplate(kafkaTemplate);
		writer.setItemKeyMapper(itemKeyMapper);
		writer.setDelete(false);
		writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		writer = new KafkaItemWriter<>();

		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalArgumentException ignore) {
		}

		writer.setKafkaTemplate(kafkaTemplate);
		try {
			writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalArgumentException ignore) {
		}

		writer.setItemKeyMapper(itemKeyMapper);
		writer.afterPropertiesSet();
	}

	@Test
	public void testBasicWrite() throws Exception {
		List<String> items = Arrays.asList("val1", "val2");

		writer.write(items);

		verify(kafkaTemplate).sendDefault(items.get(0), items.get(0));
		verify(kafkaTemplate).sendDefault(items.get(1), items.get(1));
	}

	static class KafkaItemKeyMapper implements Converter<String, String> {

		@Override
		public String convert(String source) {
			return source;
		}
	}
}