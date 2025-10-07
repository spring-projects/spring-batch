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
package org.springframework.batch.infrastructure.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.PatternMatcher;

/**
 * @author Dan Garrette
 * @since 2.0
 */
class PatternMatcherTests {

	private static final Map<String, Integer> map = new HashMap<>();

	static {
		map.put("an*", 3);
		map.put("a*", 2);
		map.put("big*", 4);
	}

	private static final Map<String, Integer> defaultMap = new HashMap<>();

	static {
		defaultMap.put("an", 3);
		defaultMap.put("a", 2);
		defaultMap.put("big*", 4);
		defaultMap.put("big?*", 5);
		defaultMap.put("*", 1);
	}

	@Test
	void testMatchNoWildcardYes() {
		assertTrue(PatternMatcher.match("abc", "abc"));
	}

	@Test
	void testMatchNoWildcardNo() {
		assertFalse(PatternMatcher.match("abc", "ab"));
	}

	@Test
	void testMatchSingleYes() {
		assertTrue(PatternMatcher.match("a?c", "abc"));
	}

	@Test
	void testMatchSingleNo() {
		assertFalse(PatternMatcher.match("a?c", "ab"));
	}

	@Test
	void testMatchSingleWildcardNo() {
		assertTrue(PatternMatcher.match("a?*", "abc"));
	}

	@Test
	void testMatchStarYes() {
		assertTrue(PatternMatcher.match("a*c", "abdegc"));
	}

	@Test
	void testMatchTwoStars() {
		assertTrue(PatternMatcher.match("a*d*", "abcdeg"));
	}

	@Test
	void testMatchPastEnd() {
		assertFalse(PatternMatcher.match("a*de", "abcdeg"));
	}

	@Test
	void testMatchPastEndTwoStars() {
		assertTrue(PatternMatcher.match("a*d*g*", "abcdeg"));
	}

	@Test
	void testMatchStarAtEnd() {
		assertTrue(PatternMatcher.match("ab*", "ab"));
	}

	@Test
	void testMatchStarNo() {
		assertFalse(PatternMatcher.match("a*c", "abdeg"));
	}

	@Test
	void testMatchPrefixSubsumed() {
		assertEquals(2, new PatternMatcher<>(map).match("apple").intValue());
	}

	@Test
	void testMatchPrefixSubsuming() {
		assertEquals(3, new PatternMatcher<>(map).match("animal").intValue());
	}

	@Test
	void testMatchPrefixUnrelated() {
		assertEquals(4, new PatternMatcher<>(map).match("biggest").intValue());
	}

	@Test
	void testMatchPrefixNoMatch() {
		PatternMatcher<Integer> matcher = new PatternMatcher<>(map);
		assertThrows(IllegalStateException.class, () -> matcher.match("bat"));
	}

	@Test
	void testMatchPrefixDefaultValueUnrelated() {
		assertEquals(5, new PatternMatcher<>(defaultMap).match("biggest").intValue());
	}

	@Test
	void testMatchPrefixDefaultValueEmptyString() {
		assertEquals(1, new PatternMatcher<>(defaultMap).match("").intValue());
	}

	@Test
	void testMatchPrefixDefaultValueNoMatch() {
		assertEquals(1, new PatternMatcher<>(defaultMap).match("bat").intValue());
	}

}
