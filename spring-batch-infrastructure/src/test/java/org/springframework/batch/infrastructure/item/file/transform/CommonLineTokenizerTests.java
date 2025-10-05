/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.infrastructure.item.file.transform;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.AbstractLineTokenizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link AbstractLineTokenizer}.
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 */
class CommonLineTokenizerTests {

	/**
	 * Columns names are considered to be specified if they are not <code>null</code> or
	 * empty.
	 */
	@Test
	void testHasNames() {
		AbstractLineTokenizer tokenizer = new AbstractLineTokenizer() {
			@Override
			protected List<String> doTokenize(String line) {
				return null;
			}
		};

		assertFalse(tokenizer.hasNames());

		tokenizer.setNames((String) null);
		assertFalse(tokenizer.hasNames());

		tokenizer.setNames(new ArrayList<String>().toArray(new String[0]));
		assertFalse(tokenizer.hasNames());

		tokenizer.setNames("name1", "name2");
		assertTrue(tokenizer.hasNames());
	}

}
