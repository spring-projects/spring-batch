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

package org.springframework.batch.item.file.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * @author Ben Hale
 * @author Dan Garrette
 * @author Dave Syer
 */
class PatternMatchingCompositeLineTokenizerTests {

	private PatternMatchingCompositeLineTokenizer tokenizer;

	@Test
	void testEmptyKeyMatchesAnyLine() {
		Map<String, LineTokenizer> map = new HashMap<>();
		map.put("*", new DelimitedLineTokenizer());
		map.put("foo", line -> null);
		tokenizer = new PatternMatchingCompositeLineTokenizer(map);
		FieldSet fields = tokenizer.tokenize("abc");
		assertEquals(1, fields.getFieldCount());
	}

	@Test
	void testEmptyKeyDoesNotMatchWhenAlternativeAvailable() {

		Map<String, LineTokenizer> map = new LinkedHashMap<>();
		map.put("*", line -> null);
		map.put("foo*", new DelimitedLineTokenizer());
		tokenizer = new PatternMatchingCompositeLineTokenizer(map);
		FieldSet fields = tokenizer.tokenize("foo,bar");
		assertEquals("bar", fields.readString(1));
	}

	@Test
	void testNoMatch() {
		tokenizer = new PatternMatchingCompositeLineTokenizer(
				Collections.singletonMap("foo", new DelimitedLineTokenizer()));
		assertThrows(IllegalStateException.class, () -> tokenizer.tokenize("nomatch"));
	}

	@Test
	void testMatchWithPrefix() {
		tokenizer = new PatternMatchingCompositeLineTokenizer(
				Collections.singletonMap("foo*", line -> new DefaultFieldSet(new String[] { line })));
		FieldSet fields = tokenizer.tokenize("foo bar");
		assertEquals(1, fields.getFieldCount());
		assertEquals("foo bar", fields.readString(0));
	}

}
