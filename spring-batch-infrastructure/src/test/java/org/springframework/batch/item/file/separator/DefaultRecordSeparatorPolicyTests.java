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

public class DefaultRecordSeparatorPolicyTests {

	DefaultRecordSeparatorPolicy policy = new DefaultRecordSeparatorPolicy();

	@Test
	public void testNormalLine() throws Exception {
		assertTrue(policy.isEndOfRecord("a string"));
	}

	@Test
	public void testQuoteUnterminatedLine() throws Exception {
		assertFalse(policy.isEndOfRecord("a string\"one"));
	}

	@Test
	public void testEmptyLine() throws Exception {
		assertTrue(policy.isEndOfRecord(""));
	}

	@Test
	public void testNullLine() throws Exception {
		assertTrue(policy.isEndOfRecord(null));
	}

	@Test
	public void testPostProcess() throws Exception {
		String line = "foo\nbar";
		assertEquals(line, policy.postProcess(line));
	}

	@Test
	public void testPreProcessWithQuote() throws Exception {
		String line = "foo\"bar";
		assertEquals(line + "\n", policy.preProcess(line));
	}

	@Test
	public void testPreProcessWithNotDefaultQuote() throws Exception {
		String line = "foo'bar";
		policy.setQuoteCharacter("'");
		assertEquals(line + "\n", policy.preProcess(line));
	}

	@Test
	public void testPreProcessWithoutQuote() throws Exception {
		String line = "foo";
		assertEquals(line, policy.preProcess(line));
	}

	@Test
	public void testContinuationMarkerNotEnd() throws Exception {
		String line = "foo\\";
		assertFalse(policy.isEndOfRecord(line));
	}

	@Test
	public void testNotDefaultContinuationMarkerNotEnd() throws Exception {
		String line = "foo bar";
		policy.setContinuation("bar");
		assertFalse(policy.isEndOfRecord(line));
	}

	@Test
	public void testContinuationMarkerRemoved() throws Exception {
		String line = "foo\\";
		assertEquals("foo", policy.preProcess(line));
	}

}
