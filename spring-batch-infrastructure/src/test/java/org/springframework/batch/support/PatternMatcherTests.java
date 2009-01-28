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
package org.springframework.batch.support;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

/**
 * @author Dan Garrette
 * @since 2.0
 */
public class PatternMatcherTests {

	private static Map<String, Integer> map = new HashMap<String, Integer>();
	static {
		map.put("an", 3);
		map.put("a", 2);
		map.put("big", 4);
	}

	private static Map<String, Integer> defaultMap = new HashMap<String, Integer>();
	static {
		defaultMap.put("an", 3);
		defaultMap.put("a", 2);
		defaultMap.put("big", 4);
		defaultMap.put("", 1);
	}

	@Test
	public void testMatch_noWildcard_yes() {
		assertTrue(PatternMatcher.match("abc", "abc"));
	}

	@Test
	public void testMatch_noWildcard_no() {
		assertFalse(PatternMatcher.match("abc", "ab"));
	}

	@Test
	public void testMatch_qMark_yes() {
		assertTrue(PatternMatcher.match("a?c", "abc"));
	}

	@Test
	public void testMatch_qMark_no() {
		assertFalse(PatternMatcher.match("a?c", "ab"));
	}

	@Test
	public void testMatch_star_yes() {
		assertTrue(PatternMatcher.match("a*c", "abdegc"));
	}

	@Test
	public void testMatch_star_no() {
		assertFalse(PatternMatcher.match("a*c", "abdeg"));
	}

	@Test
	public void testMatchPrefix_subsumed() {
		assertEquals(2, PatternMatcher.matchPrefix("apple", map).intValue());
	}

	@Test
	public void testMatchPrefix_subsuming() {
		assertEquals(3, PatternMatcher.matchPrefix("animal", map).intValue());
	}

	@Test
	public void testMatchPrefix_unrelated() {
		assertEquals(4, PatternMatcher.matchPrefix("biggest", map).intValue());
	}

	@Test(expected = IllegalStateException.class)
	public void testMatchPrefix_noMatch() {
		PatternMatcher.matchPrefix("bat", map);
	}

	@Test
	public void testMatchPrefix_defaultValue_unrelated() {
		assertEquals(4, PatternMatcher.matchPrefix("biggest", defaultMap).intValue());
	}

	@Test
	public void testMatchPrefix_defaultValue_emptyString() {
		assertEquals(1, PatternMatcher.matchPrefix("", defaultMap).intValue());
	}

	@Test
	public void testMatchPrefix_defaultValue_noMatch() {
		assertEquals(1, PatternMatcher.matchPrefix("bat", defaultMap).intValue());
	}
}
