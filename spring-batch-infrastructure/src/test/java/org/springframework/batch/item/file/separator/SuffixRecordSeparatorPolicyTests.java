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

package org.springframework.batch.item.file.separator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SuffixRecordSeparatorPolicyTests {

	private static final String LINE = "a string";

	private final SuffixRecordSeparatorPolicy policy = new SuffixRecordSeparatorPolicy();

	@Test
	void testNormalLine() {
		assertFalse(policy.isEndOfRecord(LINE));
	}

	@Test
	void testNormalLineWithDefaultSuffix() {
		assertTrue(policy.isEndOfRecord(LINE + SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX));
	}

	@Test
	void testNormalLineWithNonDefaultSuffix() {
		policy.setSuffix(":foo");
		assertTrue(policy.isEndOfRecord(LINE + ":foo"));
	}

	@Test
	void testNormalLineWithDefaultSuffixAndWhitespace() {
		assertTrue(policy.isEndOfRecord(LINE + SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX + "  "));
	}

	@Test
	void testNormalLineWithDefaultSuffixWithIgnoreWhitespace() {
		policy.setIgnoreWhitespace(false);
		assertFalse(policy.isEndOfRecord(LINE + SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX + "  "));
	}

	@Test
	void testEmptyLine() {
		assertFalse(policy.isEndOfRecord(""));
	}

	@Test
	void testPostProcessSunnyDay() {
		String line = LINE;
		String record = line + SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX;
		assertEquals(line, policy.postProcess(record));
	}

}
