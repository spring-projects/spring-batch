/*
 * Copyright 2019-2021 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.springframework.batch.item.kafka;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

public class KafkaItemWriterTests {

	@Rule
	public MockitoRule rule = MockitoJUnit.rule().silent();
	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	@Mock
	private ListenableFuture<SendResult<String, String>> future;

	private KafkaItemKeyMapper itemKeyMapper;

	private KafkaItemWriter<String, String> writer;

	@Before
	public void setUp() throws Exception {
		when(this.kafkaTemplate.getDefaultTopic()).thenReturn("defaultTopic");
		when(this.kafkaTemplate.sendDefault(any(), any())).thenReturn(this.future);
		this.itemKeyMapper = new KafkaItemKeyMapper();
		this.writer = new KafkaItemWriter<>();
		this.writer.setKafkaTemplate(this.kafkaTemplate);
		this.writer.setItemKeyMapper(this.itemKeyMapper);
		this.writer.setDelete(false);
		this.writer.setTimeout(10L);
		this.writer.afterPropertiesSet();
	}

	@Test
	public void testAfterPropertiesSet() throws Exception {
		this.writer = new KafkaItemWriter<>();

		try {
			this.writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalArgumentException exception) {
			assertEquals("itemKeyMapper requires a Converter type.", exception.getMessage());
		}

		this.writer.setItemKeyMapper(this.itemKeyMapper);
		try {
			this.writer.afterPropertiesSet();
			fail("Expected exception was not thrown");
		}
		catch (IllegalArgumentException exception) {
			assertEquals("KafkaTemplate must not be null.", exception.getMessage());
		}

		this.writer.setKafkaTemplate(this.kafkaTemplate);
		try {
			this.writer.afterPropertiesSet();
		}
		catch (Exception e) {
			fail("Must not throw an exception when correctly configured");
		}
	}

	@Test
	public void testBasicWrite() throws Exception {
		List<String> items = Arrays.asList("val1", "val2");

		this.writer.write(items);

		verify(this.kafkaTemplate).sendDefault(items.get(0), items.get(0));
		verify(this.kafkaTemplate).sendDefault(items.get(1), items.get(1));
		verify(this.kafkaTemplate).flush();
		verify(this.future, times(2)).get(10L, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testBasicDelete() throws Exception {
		List<String> items = Arrays.asList("val1", "val2");
		this.writer.setDelete(true);

		this.writer.write(items);

		verify(this.kafkaTemplate).sendDefault(items.get(0), null);
		verify(this.kafkaTemplate).sendDefault(items.get(1), null);
		verify(this.kafkaTemplate).flush();
		verify(this.future, times(2)).get(10L, TimeUnit.MILLISECONDS);
	}

	@Test
	public void testKafkaTemplateCanBeReferencedFromSubclass() {
		KafkaItemWriter<String, String> kafkaItemWriter = new KafkaItemWriter<String, String>() {
			@Override
			protected void writeKeyValue(String key, String value) {
				this.kafkaTemplate.sendDefault(key, value);
			}
		};
		kafkaItemWriter.setKafkaTemplate(this.kafkaTemplate);
		kafkaItemWriter.writeKeyValue("k", "v");
		verify(this.kafkaTemplate).sendDefault("k", "v");
	}

	static class KafkaItemKeyMapper implements Converter<String, String> {

		@Override
		public String convert(String source) {
			return source;
		}
	}
}
