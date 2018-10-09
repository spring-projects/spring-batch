/*
 * Copyright 2008-2014 the original author or authors.
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
package org.springframework.batch.sample.domain.multiline;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;

public class AggregateItemFieldSetMapperTests {
	private AggregateItemFieldSetMapper<String> mapper = new AggregateItemFieldSetMapper<>();

	@Test
	public void testDefaultBeginRecord() throws Exception {
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "BEGIN" })).isHeader());
		assertFalse(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "BEGIN" })).isFooter());
	}

	@Test
	public void testSetBeginRecord() throws Exception {
		mapper.setBegin("FOO");
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).isHeader());
	}

	@Test
	public void testDefaultEndRecord() throws Exception {
		assertFalse(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "END" })).isHeader());
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "END" })).isFooter());
	}

	@Test
	public void testSetEndRecord() throws Exception {
		mapper.setEnd("FOO");
		assertTrue(mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).isFooter());
	}

	@Test
	public void testMandatoryProperties() throws Exception {
		try {
			mapper.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testDelegate() throws Exception {
		mapper.setDelegate(new FieldSetMapper<String>() {
			@Override
			public String mapFieldSet(FieldSet fs) {
				return "foo";
			}
		});
		assertEquals("foo", mapper.mapFieldSet(new DefaultFieldSet(new String[] { "FOO" })).getItem());
	}
}
