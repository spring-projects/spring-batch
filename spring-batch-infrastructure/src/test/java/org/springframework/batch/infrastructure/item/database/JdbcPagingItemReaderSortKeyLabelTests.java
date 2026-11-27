/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.infrastructure.item.database;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.database.JdbcPagingItemReader.SortKeyLabel;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the resolution of a sort key to a result set column label, covering the
 * cases that a database round trip cannot easily reach.
 *
 * @author Jinhui Kim
 */
class JdbcPagingItemReaderSortKeyLabelTests {

	@Test
	void unquotedQualifiedKey() {
		assertSortKeyLabel("p.id", new SortKeyLabel("id", false, false));
	}

	@Test
	void plainKey() {
		assertSortKeyLabel("id", new SortKeyLabel("id", false, true));
	}

	@Test
	void plainKeyWithOuterSpaces() {
		assertSortKeyLabel("  id  ", new SortKeyLabel("id", false, true));
	}

	@Test
	void quotedKeyWithOuterSpaces() {
		assertSortKeyLabel("  \"A\"  ", new SortKeyLabel("A", true, false));
	}

	@Test
	void quotedKeyContainingDot() {
		assertSortKeyLabel("\"a.b\"", new SortKeyLabel("a.b", true, false));
	}

	@Test
	void quotedKeyContainingEscapedQuote() {
		assertSortKeyLabel("\"A\"\"B\"", new SortKeyLabel("A\"B", true, false));
	}

	@Test
	void quotedQualifierWithUnquotedColumn() {
		assertSortKeyLabel("\"t\".id", new SortKeyLabel("id", false, false));
	}

	@Test
	void fullyQuotedQualifiedKey() {
		assertSortKeyLabel("\"t\".\"A\"", new SortKeyLabel("A", true, false));
	}

	@Test
	void quotedKeyContainingSpaces() {
		assertSortKeyLabel("\" A \"", new SortKeyLabel(" A ", true, false));
	}

	private void assertSortKeyLabel(String key, SortKeyLabel expected) {
		assertEquals(expected, JdbcPagingItemReader.getSortKeyLabel(key));
	}

}
