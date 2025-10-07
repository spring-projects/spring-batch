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

package org.springframework.batch.infrastructure.item.file.separator;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.separator.DefaultRecordSeparatorPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultRecordSeparatorPolicyTests {

	private final DefaultRecordSeparatorPolicy policy = new DefaultRecordSeparatorPolicy();

	@Test
	void testNormalLine() {
		assertTrue(policy.isEndOfRecord("a string"));
	}

	@Test
	void testQuoteUnterminatedLine() {
		assertFalse(policy.isEndOfRecord("a string\"one"));
	}

	@Test
	void testEmptyLine() {
		assertTrue(policy.isEndOfRecord(""));
	}

	@Test
	void testNullLine() {
		assertTrue(policy.isEndOfRecord(null));
	}

	@Test
	void testPostProcess() {
		String line = "foo\nbar";
		assertEquals(line, policy.postProcess(line));
	}

	@Test
	void testPreProcessWithQuote() {
		String line = "foo\"bar";
		assertEquals(line + "\n", policy.preProcess(line));
	}

	@Test
	void testPreProcessWithNotDefaultQuote() {
		String line = "foo'bar";
		policy.setQuoteCharacter("'");
		assertEquals(line + "\n", policy.preProcess(line));
	}

	@Test
	void testPreProcessWithoutQuote() {
		String line = "foo";
		assertEquals(line, policy.preProcess(line));
	}

	@Test
	void testContinuationMarkerNotEnd() {
		String line = "foo\\";
		assertFalse(policy.isEndOfRecord(line));
	}

	@Test
	void testNotDefaultContinuationMarkerNotEnd() {
		String line = "foo bar";
		policy.setContinuation("bar");
		assertFalse(policy.isEndOfRecord(line));
	}

	@Test
	void testContinuationMarkerRemoved() {
		String line = "foo\\";
		assertEquals("foo", policy.preProcess(line));
	}

}
