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

package org.springframework.batch.item.file.separator;

import junit.framework.TestCase;

public class SuffixRecordSeparatorPolicyTests extends TestCase {

	private static final String LINE = "a string";
	SuffixRecordSeparatorPolicy policy = new SuffixRecordSeparatorPolicy();
	
	public void testNormalLine() throws Exception {
		assertFalse(policy.isEndOfRecord(LINE));
	}

	public void testNormalLineWithDefaultSuffix() throws Exception {
		assertTrue(policy.isEndOfRecord(LINE+SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX));
	}

	public void testNormalLineWithNonDefaultSuffix() throws Exception {
		policy.setSuffix(":foo");
		assertTrue(policy.isEndOfRecord(LINE+ ":foo"));
	}

	public void testNormalLineWithDefaultSuffixAndWhitespace() throws Exception {
		assertTrue(policy.isEndOfRecord(LINE+SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX+"  "));
	}

	public void testNormalLineWithDefaultSuffixWithIgnoreWhitespace() throws Exception {
		policy.setIgnoreWhitespace(false);
		assertFalse(policy.isEndOfRecord(LINE+SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX+"  "));
	}

	public void testEmptyLine() throws Exception {
		assertFalse(policy.isEndOfRecord(""));
	}

	public void testNullLineIsEndOfRecord() throws Exception {
		assertTrue(policy.isEndOfRecord(null));
	}
	
	public void testPostProcessSunnyDay() throws Exception {
		String line = LINE;
		String record = line+SuffixRecordSeparatorPolicy.DEFAULT_SUFFIX;
		assertEquals(line, policy.postProcess(record));		
	}

	public void testPostProcessNullLine() throws Exception {
		String line = null;
		assertEquals(null, policy.postProcess(line));		
	}

}
