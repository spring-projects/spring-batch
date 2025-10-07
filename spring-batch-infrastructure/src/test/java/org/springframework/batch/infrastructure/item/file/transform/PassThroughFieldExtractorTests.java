/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;
import org.springframework.batch.infrastructure.item.file.transform.PassThroughFieldExtractor;

/**
 * @author Dan Garrette
 * @since 2.0
 */
class PassThroughFieldExtractorTests {

	@Test
	void testExtractString() {
		PassThroughFieldExtractor<String> extractor = new PassThroughFieldExtractor<>();
		Object[] result = extractor.extract("abc");
		assertArrayEquals(new Object[] { "abc" }, result);
	}

	@Test
	void testExtractArray() {
		PassThroughFieldExtractor<String[]> extractor = new PassThroughFieldExtractor<>();
		Object[] result = extractor.extract(new String[] { "a", "b", null, "d" });
		assertArrayEquals(new Object[] { "a", "b", null, "d" }, result);
	}

	@Test
	void testExtractFieldSet() {
		PassThroughFieldExtractor<FieldSet> extractor = new PassThroughFieldExtractor<>();
		Object[] result = extractor.extract(new DefaultFieldSet(new String[] { "a", "b", "", "d" }));
		assertArrayEquals(new Object[] { "a", "b", "", "d" }, result);
	}

	@Test
	void testExtractCollection() {
		PassThroughFieldExtractor<List<String>> extractor = new PassThroughFieldExtractor<>();
		Object[] result = extractor.extract(Arrays.asList("a", "b", null, "d"));
		assertArrayEquals(new Object[] { "a", "b", null, "d" }, result);
	}

	@Test
	void testExtractMap() {
		PassThroughFieldExtractor<Map<String, String>> extractor = new PassThroughFieldExtractor<>();
		Map<String, String> map = new LinkedHashMap<>();
		map.put("A", "a");
		map.put("B", "b");
		map.put("C", null);
		map.put("D", "d");
		Object[] result = extractor.extract(map);
		assertArrayEquals(new Object[] { "a", "b", null, "d" }, result);
	}

}
