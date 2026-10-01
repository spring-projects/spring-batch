/*
 * Copyright 2019-2026 the original author or authors.
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

package org.springframework.batch.infrastructure.item.kafka;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Mathieu Ouellet
 * @author Mahmoud Ben Hassine
 * @author djechelon@github.com
 */
class KafkaConsumerRecordItemReaderTests {

	@Test
	void testValidation() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaConsumerRecordItemReader<>(null, "topic", 0));
		assertEquals("Consumer properties must not be null", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaConsumerRecordItemReader<>(new Properties(), "topic", 0));
		assertEquals("bootstrap.servers property must be provided", exception.getMessage());

		Properties consumerProperties = new Properties();
		consumerProperties.put("bootstrap.servers", "mockServer");
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaConsumerRecordItemReader<>(consumerProperties, "topic", 0));
		assertEquals("group.id property must be provided", exception.getMessage());

		consumerProperties.put("group.id", "1");
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaConsumerRecordItemReader<>(consumerProperties, "topic", 0));
		assertEquals("key.deserializer property must be provided", exception.getMessage());

		consumerProperties.put("key.deserializer", StringDeserializer.class.getName());
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaConsumerRecordItemReader<>(consumerProperties, "topic", 0));
		assertEquals("value.deserializer property must be provided", exception.getMessage());

		consumerProperties.put("value.deserializer", StringDeserializer.class.getName());
		exception = assertThrows(IllegalArgumentException.class,
				() -> new KafkaConsumerRecordItemReader<>(consumerProperties, "", 0));
		assertEquals("Topic name must not be null or empty", exception.getMessage());

		exception = assertThrows(Exception.class,
				() -> new KafkaConsumerRecordItemReader<>(consumerProperties, "topic"));
		assertEquals("At least one partition must be provided", exception.getMessage());

		KafkaConsumerRecordItemReader<String, String> reader = new KafkaConsumerRecordItemReader<>(consumerProperties,
				"topic", 0);

		exception = assertThrows(IllegalArgumentException.class, () -> reader.setPollTimeout(null));
		assertEquals("pollTimeout must not be null", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class, () -> reader.setPollTimeout(Duration.ZERO));
		assertEquals("pollTimeout must not be zero", exception.getMessage());

		exception = assertThrows(IllegalArgumentException.class, () -> reader.setPollTimeout(Duration.ofSeconds(-1)));
		assertEquals("pollTimeout must not be negative", exception.getMessage());
	}

}
