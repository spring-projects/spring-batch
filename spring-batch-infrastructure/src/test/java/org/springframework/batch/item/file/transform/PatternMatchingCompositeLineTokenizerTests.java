/*
 * Copyright 2006-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.lang.Nullable;

/**
 * @author Ben Hale
 * @author Dan Garrette
 * @author Dave Syer
 */
public class PatternMatchingCompositeLineTokenizerTests {

	private PatternMatchingCompositeLineTokenizer tokenizer = new PatternMatchingCompositeLineTokenizer();

	@Test(expected = IllegalArgumentException.class)
	public void testNoTokenizers() throws Exception {
		tokenizer.afterPropertiesSet();
		tokenizer.tokenize("a line");
	}

	@Test
	public void testEmptyKeyMatchesAnyLine() throws Exception {
		Map<String, LineTokenizer> map = new HashMap<>();
		map.put("*", new DelimitedLineTokenizer());
		map.put("foo", new LineTokenizer() {
            @Override
			public FieldSet tokenize(@Nullable String line) {
				return null;
			}
		});
		tokenizer.setTokenizers(map);
		tokenizer.afterPropertiesSet();
		FieldSet fields = tokenizer.tokenize("abc");
		assertEquals(1, fields.getFieldCount());
	}

	@Test
	public void testEmptyKeyDoesNotMatchWhenAlternativeAvailable() throws Exception {

		Map<String, LineTokenizer> map = new LinkedHashMap<>();
		map.put("*", new LineTokenizer() {
            @Override
			public FieldSet tokenize(@Nullable String line) {
				return null;
			}
		});
		map.put("foo*", new DelimitedLineTokenizer());
		tokenizer.setTokenizers(map);
		tokenizer.afterPropertiesSet();
		FieldSet fields = tokenizer.tokenize("foo,bar");
		assertEquals("bar", fields.readString(1));
	}

	@Test(expected = IllegalStateException.class)
	public void testNoMatch() throws Exception {
		tokenizer.setTokenizers(Collections.singletonMap("foo", (LineTokenizer) new DelimitedLineTokenizer()));
		tokenizer.afterPropertiesSet();
		tokenizer.tokenize("nomatch");
	}

	@Test
	public void testMatchWithPrefix() throws Exception {
		tokenizer.setTokenizers(Collections.singletonMap("foo*", (LineTokenizer) new LineTokenizer() {
            @Override
			public FieldSet tokenize(@Nullable String line) {
				return new DefaultFieldSet(new String[] { line });
			}
		}));
		tokenizer.afterPropertiesSet();
		FieldSet fields = tokenizer.tokenize("foo bar");
		assertEquals(1, fields.getFieldCount());
		assertEquals("foo bar", fields.readString(0));
	}
}
