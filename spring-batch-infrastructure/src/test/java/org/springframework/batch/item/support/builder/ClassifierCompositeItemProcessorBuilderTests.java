/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.batch.item.support.builder;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.ClassifierCompositeItemProcessor;
import org.springframework.classify.PatternMatchingClassifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Glenn Renfro
 */
public class ClassifierCompositeItemProcessorBuilderTests {

	@Test
	public void testBasicClassifierCompositeItemProcessor() throws Exception {

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
	public void testNullClassifier() {
		try {
			new ClassifierCompositeItemProcessorBuilder<String, String>().build();
			fail("IllegalArgumentException should have been thrown");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("IllegalArgumentException message did not match the expected result.",
					"A classifier is required.", iae.getMessage());
		}
	}
}
