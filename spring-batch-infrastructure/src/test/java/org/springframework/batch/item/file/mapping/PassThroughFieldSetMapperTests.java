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
package org.springframework.batch.item.file.mapping;

import org.springframework.batch.item.file.transform.DefaultFieldSet;
import org.springframework.batch.item.file.transform.FieldSet;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 * 
 */
public class PassThroughFieldSetMapperTests extends TestCase {

	private PassThroughFieldSetMapper mapper = new PassThroughFieldSetMapper();

	/**
	 * Test method for
	 * {@link org.springframework.batch.item.file.mapping.PassThroughFieldSetMapper#mapFieldSet(org.springframework.batch.item.file.transform.FieldSet)}.
	 */
	public void testMapLine() {
		FieldSet fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertEquals(fieldSet, mapper.mapFieldSet(fieldSet));
	}

	
}
