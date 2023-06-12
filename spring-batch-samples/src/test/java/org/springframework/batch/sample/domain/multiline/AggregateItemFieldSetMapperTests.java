/*
 * Copyright 2008-2023 the original author or authors.
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
package org.springframework.batch.sample.domain.multiline;

import org.junit.jupiter.api.Test;
import org.springframework.batch.item.file.transform.DefaultFieldSet;

import static org.junit.jupiter.api.Assertions.*;

class AggregateItemFieldSetMapperTests {

	private final AggregateItemFieldSetMapper<String> mapper = new AggregateItemFieldSetMapper<>();

	@Test
	void testDefaultBeginRecord() throws Exception {
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "BEGIN" })).isHeader());
		assertFalse(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "BEGIN" })).isFooter());
	}

	@Test
	void testSetBeginRecord() throws Exception {
		mapper.setBegin("FOO");
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).isHeader());
	}

	@Test
	void testDefaultEndRecord() throws Exception {
		assertFalse(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "END" })).isHeader());
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "END" })).isFooter());
	}

	@Test
	void testSetEndRecord() throws Exception {
		mapper.setEnd("FOO");
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).isFooter());
	}

	@Test
	void testMandatoryProperties() {
		assertThrows(IllegalStateException.class, mapper::afterPropertiesSet);
	}

	@Test
	void testDelegate() throws Exception {
		mapper.setDelegate(fs -> "foo");
		assertEquals("foo", mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).getItem());
	}

}
