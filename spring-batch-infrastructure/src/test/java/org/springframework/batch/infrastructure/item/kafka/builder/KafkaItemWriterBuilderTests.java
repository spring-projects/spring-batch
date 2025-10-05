/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.kafka.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.infrastructure.item.kafka.KafkaItemWriter;
import org.springframework.batch.infrastructure.item.kafka.builder.KafkaItemWriterBuilder;
import org.springframework.core.convert.converter.Converter;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 */
@ExtendWith(MockitoExtension.class)
class KafkaItemWriterBuilderTests {

	@Mock
	private KafkaTemplate<String, String> kafkaTemplate;

	private KafkaItemKeyMapper itemKeyMapper;

	@BeforeEach
	void setUp() {
		this.itemKeyMapper = new KafkaItemKeyMapper();
	}

	@Test
	void testNullKafkaTemplate() {
		// given
		final KafkaItemWriterBuilder<String, String> builder = new KafkaItemWriterBuilder<String, String>()
			.itemKeyMapper(this.itemKeyMapper);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("kafkaTemplate is required.");
	}

	@Test
	void testNullItemKeyMapper() {
		// given
		final KafkaItemWriterBuilder<String, String> builder = new KafkaItemWriterBuilder<String, String>()
			.kafkaTemplate(this.kafkaTemplate);

		// when
		final Exception expectedException = assertThrows(IllegalArgumentException.class, builder::build);

		// then
		assertThat(expectedException).hasMessage("itemKeyMapper is required.");
	}

	@Test
	void testKafkaItemWriterBuild() {
		// given
		boolean delete = true;
		long timeout = 10L;

		// when
		KafkaItemWriter<String, String> writer = new KafkaItemWriterBuilder<String, String>()
			.kafkaTemplate(this.kafkaTemplate)
			.itemKeyMapper(this.itemKeyMapper)
			.delete(delete)
			.timeout(timeout)
			.build();

		// then
		assertTrue((Boolean) ReflectionTestUtils.getField(writer, "delete"));
		assertEquals(timeout, ReflectionTestUtils.getField(writer, "timeout"));
		assertEquals(this.itemKeyMapper, ReflectionTestUtils.getField(writer, "itemKeyMapper"));
		assertEquals(this.kafkaTemplate, ReflectionTestUtils.getField(writer, "kafkaTemplate"));
	}

	static class KafkaItemKeyMapper implements Converter<String, String> {

		@Override
		public String convert(String source) {
			return source;
		}

	}

}
