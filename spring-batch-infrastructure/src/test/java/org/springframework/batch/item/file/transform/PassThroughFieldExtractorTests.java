/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.item.file.transform;

import static junit.framework.Assert.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class PassThroughFieldExtractorTests {

	@Test
	public void testExtractString() {
		PassThroughFieldExtractor<String> extractor = new PassThroughFieldExtractor<String>();
		Object[] result = extractor.extract("abc");
		assertTrue(Arrays.equals(new Object[] { "abc" }, result));
	}

	@Test
	public void testExtractArray() {
		PassThroughFieldExtractor<String[]> extractor = new PassThroughFieldExtractor<String[]>();
		Object[] result = extractor.extract(new String[] { "a", "b", null, "d" });
		assertTrue(Arrays.equals(new Object[] { "a", "b", null, "d" }, result));
	}

	@Test
	public void testExtractFieldSet() {
		PassThroughFieldExtractor<FieldSet> extractor = new PassThroughFieldExtractor<FieldSet>();
		Object[] result = extractor.extract(new DefaultFieldSet(new String[] { "a", "b", "", "d" }));
		assertTrue(Arrays.equals(new Object[] { "a", "b", "", "d" }, result));
	}

	@Test
	public void testExtractCollection() {
		PassThroughFieldExtractor<List<String>> extractor = new PassThroughFieldExtractor<List<String>>();
		Object[] result = extractor.extract(Arrays.asList("a", "b", null, "d"));
		assertTrue(Arrays.equals(new Object[] { "a", "b", null, "d" }, result));
	}

	@Test
	public void testExtractMap() {
		PassThroughFieldExtractor<Map<String, String>> extractor = new PassThroughFieldExtractor<Map<String, String>>();
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("A", "a");
		map.put("B", "b");
		map.put("C", null);
		map.put("D", "d");
		Object[] result = extractor.extract(map);
		assertTrue(Arrays.equals(new Object[] { "a", "b", null, "d" }, result));
	}

}
