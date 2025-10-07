/*
 * Copyright 2017-2022 the original author or authors.
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

package org.springframework.batch.infrastructure.item.support.builder;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.support.ClassifierCompositeItemProcessor;
import org.springframework.batch.infrastructure.item.support.builder.ClassifierCompositeItemProcessorBuilder;
import org.springframework.classify.PatternMatchingClassifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Glenn Renfro
 */
class ClassifierCompositeItemProcessorBuilderTests {

	@Test
	void testBasicClassifierCompositeItemProcessor() throws Exception {

		ItemProcessor<String, String> fooProcessor = item -> "foo: " + item;
		ItemProcessor<String, String> defaultProcessor = item -> item;

		Map<String, ItemProcessor<?, ? extends String>> routingConfiguration = new HashMap<>();
		routingConfiguration.put("foo", fooProcessor);
		routingConfiguration.put("*", defaultProcessor);
		ClassifierCompositeItemProcessor<String, String> processor = new ClassifierCompositeItemProcessorBuilder<String, String>()
			.classifier(new PatternMatchingClassifier<>(routingConfiguration))
			.build();

		assertEquals("bar", processor.process("bar"));
		assertEquals("foo: foo", processor.process("foo"));
		assertEquals("baz", processor.process("baz"));
	}

	@Test
	void testNullClassifier() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> new ClassifierCompositeItemProcessorBuilder<String, String>().build());
		assertEquals("A classifier is required.", exception.getMessage());
	}

}
