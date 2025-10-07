/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.infrastructure.item.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.support.ClassifierCompositeItemWriter;
import org.springframework.classify.PatternMatchingClassifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Dave Syer
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 *
 */
class ClassifierCompositeItemWriterTests {

	private final ClassifierCompositeItemWriter<String> writer = new ClassifierCompositeItemWriter<>();

	private final Chunk defaults = new Chunk<>();

	private final Chunk foos = new Chunk<>();

	@Test
	void testWrite() throws Exception {
		Map<String, ItemWriter<String>> map = new HashMap<>();
		ItemWriter<String> fooWriter = chunk -> foos.addAll(chunk.getItems());
		ItemWriter<String> defaultWriter = chunk -> defaults.addAll(chunk.getItems());
		map.put("foo", fooWriter);
		map.put("*", defaultWriter);
		writer.setClassifier(new PatternMatchingClassifier(map));
		writer.write(Chunk.of("foo", "foo", "one", "two", "three"));
		assertIterableEquals(Chunk.of("foo", "foo"), foos);
		assertIterableEquals(Chunk.of("one", "two", "three"), defaults);
	}

	@Test
	void testSetNullClassifier() {
		ClassifierCompositeItemWriter<String> writer = new ClassifierCompositeItemWriter<>();
		Exception exception = assertThrows(IllegalArgumentException.class, () -> writer.setClassifier(null));
		assertEquals("A classifier is required.", exception.getMessage());
	}

}
