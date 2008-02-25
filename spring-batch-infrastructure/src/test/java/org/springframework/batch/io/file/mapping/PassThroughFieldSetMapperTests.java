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
package org.springframework.batch.io.file.mapping;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 * 
 */
public class PassThroughFieldSetMapperTests extends TestCase {

	private PassThroughFieldSetMapper mapper = new PassThroughFieldSetMapper();

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.mapping.PassThroughFieldSetMapper#mapLine(org.springframework.batch.io.file.mapping.FieldSet)}.
	 */
	public void testMapLine() {
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertEquals(fieldSet, mapper.mapLine(fieldSet));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.mapping.PassThroughFieldSetMapper#mapItem(java.lang.Object)}.
	 */
	public void testUnmapItemAsFieldSet() {
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertEquals(fieldSet, mapper.mapItem(fieldSet));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.mapping.PassThroughFieldSetMapper#mapItem(java.lang.Object)}.
	 */
	public void testUnmapItemAsString() {
		assertEquals(new DefaultFieldSet(new String[] { "foo" }), mapper.mapItem("foo"));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.io.file.mapping.PassThroughFieldSetMapper#mapItem(java.lang.Object)}.
	 */
	public void testUnmapItemAsNonString() {
		Object object = new Object();
		assertEquals(new DefaultFieldSet(new String[] { "" + object }), mapper.mapItem(object));
	}
}
