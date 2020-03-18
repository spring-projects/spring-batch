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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ClassifierCompositeItemWriter;
import org.springframework.classify.PatternMatchingClassifier;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;

/**
 * @author Glenn Renfro
 */
public class ClassifierCompositeItemWriterBuilderTests {

	private List<String> defaults = new ArrayList<>();

	private List<String> foos = new ArrayList<>();

	@Test
	public void testWrite() throws Exception {
		Map<String, ItemWriter<? super String>> map = new HashMap<>();
		ItemWriter<String> fooWriter = items -> foos.addAll(items);
		ItemWriter<String> defaultWriter = items -> defaults.addAll(items);
		map.put("foo", fooWriter);
		map.put("*", defaultWriter);
		ClassifierCompositeItemWriter<String> writer = new ClassifierCompositeItemWriterBuilder<String>()
				.classifier(new PatternMatchingClassifier<>(map)).build();

		writer.write(Arrays.asList("foo", "foo", "one", "two", "three"));
		assertEquals("[foo, foo]", foos.toString());
		assertEquals("[one, two, three]", defaults.toString());
	}

	@Test
	public void testSetNullClassifier() throws Exception {
		try {
			new ClassifierCompositeItemWriterBuilder<>().build();
			fail("A classifier is required.");
		}
		catch (IllegalArgumentException iae) {
			assertEquals("Message returned from exception did not match expected result.", "A classifier is required.",
					iae.getMessage());
		}
	}
}
