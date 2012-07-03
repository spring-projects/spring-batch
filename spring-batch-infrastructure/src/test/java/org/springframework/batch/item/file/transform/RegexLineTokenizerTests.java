/*
 * Copyright 2006-2012 the original author or authors.
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

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

public class RegexLineTokenizerTests {

	private RegexLineTokenizer tokenizer = new RegexLineTokenizer();

	@Test
	public void testCapturingGroups() {
		String line = "Liverpool, England: 53d 25m 0s N 3d 0m 0s";
		tokenizer.setRegex("([a-zA-Z]+), ([a-zA-Z]+): ([0-9]+). ([0-9]+). ([0-9]+). ([A-Z]) ([0-9]+). ([0-9]+). ([0-9]+).");
		List<String> tokens = tokenizer.doTokenize(line);
		assertEquals(9, tokens.size());
		assertEquals("England", tokens.get(1));
		assertEquals("3", tokens.get(6));
	}

	@Test
	public void testNonCapturingGroups() {
		String line = "Graham James Edward Miller";
		tokenizer.setRegex("(.*?)(?: .*)* (.*)");
		List<String> tokens = tokenizer.doTokenize(line);
		assertEquals(2, tokens.size());
		assertEquals("Graham", tokens.get(0));
		assertEquals("Miller", tokens.get(1));
	}

	@Test
	public void testNoMatch() {
		tokenizer.setRegex("([0-9]+).");
		List<String> tokens = tokenizer.doTokenize("noNumber");
		assertEquals(0, tokens.size());
	}
}
