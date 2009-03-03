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
		map.put("an*", 3);
		map.put("a*", 2);
		map.put("big*", 4);
	}

	private static Map<String, Integer> defaultMap = new HashMap<String, Integer>();
	static {
		defaultMap.put("an", 3);
		defaultMap.put("a", 2);
		defaultMap.put("big*", 4);
		defaultMap.put("big?*", 5);
		defaultMap.put("*", 1);
	}

	@Test
	public void testMatchNoWildcardYes() {
		assertTrue(PatternMatcher.match("abc", "abc"));
	}

	@Test
	public void testMatchNoWildcardNo() {
		assertFalse(PatternMatcher.match("abc", "ab"));
	}

	@Test
	public void testMatchSingleYes() {
		assertTrue(PatternMatcher.match("a?c", "abc"));
	}

	@Test
	public void testMatchSingleNo() {
		assertFalse(PatternMatcher.match("a?c", "ab"));
	}

	@Test
	public void testMatchSingleWildcardNo() {
		assertTrue(PatternMatcher.match("a?*", "abc"));
	}

	@Test
	public void testMatchStarYes() {
		assertTrue(PatternMatcher.match("a*c", "abdegc"));
	}

	@Test
	public void testMatchTwoStars() {
		assertTrue(PatternMatcher.match("a*d*", "abcdeg"));
	}

	@Test
	public void testMatchPastEnd() {
		assertFalse(PatternMatcher.match("a*de", "abcdeg"));
	}

	@Test
	public void testMatchPastEndTwoStars() {
		assertTrue(PatternMatcher.match("a*d*g*", "abcdeg"));
	}

	@Test
	public void testMatchStarAtEnd() {
		assertTrue(PatternMatcher.match("ab*", "ab"));
	}

	@Test
	public void testMatchStarNo() {
		assertFalse(PatternMatcher.match("a*c", "abdeg"));
	}

	@Test
	public void testMatchPrefixSubsumed() {
		assertEquals(2, new PatternMatcher<Integer>(map).match("apple").intValue());
	}

	@Test
	public void testMatchPrefixSubsuming() {
		assertEquals(3, new PatternMatcher<Integer>(map).match("animal").intValue());
	}

	@Test
	public void testMatchPrefixUnrelated() {
		assertEquals(4, new PatternMatcher<Integer>(map).match("biggest").intValue());
	}

	@Test(expected = IllegalStateException.class)
	public void testMatchPrefixNoMatch() {
		new PatternMatcher<Integer>(map).match("bat");
	}

	@Test
	public void testMatchPrefixDefaultValueUnrelated() {
		assertEquals(5, new PatternMatcher<Integer>(defaultMap).match("biggest").intValue());
	}

	@Test
	public void testMatchPrefixDefaultValueEmptyString() {
		assertEquals(1, new PatternMatcher<Integer>(defaultMap).match("").intValue());
	}

	@Test
	public void testMatchPrefixDefaultValueNoMatch() {
		assertEquals(1, new PatternMatcher<Integer>(defaultMap).match("bat").intValue());
	}
}
