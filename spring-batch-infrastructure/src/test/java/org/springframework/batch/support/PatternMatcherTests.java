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
package org.springframework.batch.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.Test;

/**
 * @author Dan Garrette
 * @author Injae Kim
 * @since 2.0
 */
class PatternMatcherTests {

	private static final Map<String, Integer> map = new HashMap<>();

	static {
		map.put("an*", 3);
		map.put("a*", 2);
		map.put("big*", 4);
		map.put("bcd.*", 5);
	}

	private static final Map<String, Integer> defaultMap = new HashMap<>();

	static {
		defaultMap.put("an", 3);
		defaultMap.put("a", 2);
		defaultMap.put("big*", 4);
		defaultMap.put("big?*", 5);
		defaultMap.put("*", 1);
	}

	private static final Map<String, Integer> regexMap = new HashMap<>();

	static {
		regexMap.put("abc.*", 1);
		regexMap.put("a...e", 2);
		regexMap.put("123.[0-9][0-9]\\d", 3);
		regexMap.put("*............", 100); // invalid regex format
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
	void testMatchRegex() {
		assertTrue(PatternMatcher.matchRegex("abc.*", "abcde"));
	}

	@Test
	void testMatchRegex_notMatched() {
		assertFalse(PatternMatcher.matchRegex("abc.*", "cdefg"));
		assertFalse(PatternMatcher.matchRegex("abc.", "abcde"));
	}

	@Test
	void testMatchRegex_thrown_invalidRegexFormat() {
		assertThrows(PatternSyntaxException.class, () -> PatternMatcher.matchRegex("*..", "abc"));
	}

	@Test
	void testMatchRegex_thrown_notNullParam() {
		assertThrows(IllegalArgumentException.class, () -> PatternMatcher.matchRegex("regex", null));
		assertThrows(IllegalArgumentException.class, () -> PatternMatcher.matchRegex(null, "str"));
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
	void testMatchByRegex() {
		assertEquals(5, new PatternMatcher<>(map).match("bcdef12345").intValue());
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

	@Test
	void testMatchRegexPrefix() {
		assertEquals(1, new PatternMatcher<>(regexMap).match("abcdefg").intValue());
	}

	@Test
	void testMatchRegexWildCards() {
		assertEquals(2, new PatternMatcher<>(regexMap).match("a12De").intValue());
	}

	@Test
	void testMatchRegexDigits() {
		assertEquals(3, new PatternMatcher<>(regexMap).match("123-789").intValue());
	}

	@Test
	void testMatchRegexNotMatched() {
		assertThrows(IllegalStateException.class, () -> new PatternMatcher<>(regexMap).match("Hello world!"));
	}

}
