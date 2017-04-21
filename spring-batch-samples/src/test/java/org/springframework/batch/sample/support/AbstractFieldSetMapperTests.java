/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.batch.sample.support;

import org.junit.Test;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

import static org.junit.Assert.assertEquals;

/**
 * Encapsulates basic logic for testing custom {@link FieldSetMapper} implementations.
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractFieldSetMapperTests {

	/**
	 * @return <code>FieldSet</code> used for mapping
	 */
	protected abstract FieldSet fieldSet();
	
	/**
	 * @return domain object excepted as a result of mapping the <code>FieldSet</code>
	 * returned by <code>this.fieldSet()</code>
	 */
	protected abstract Object expectedDomainObject();
	
	/**
	 * @return mapper which takes <code>this.fieldSet()</code> and maps it to
	 * domain object.
	 */
	protected abstract FieldSetMapper<?> fieldSetMapper();
	
	
	/**
	 * Regular usage scenario.
	 * Assumes the domain object implements sensible <code>equals(Object other)</code>
	 */
	@Test
	public void testRegularUse() throws Exception {
		assertEquals(expectedDomainObject(), fieldSetMapper().mapFieldSet(fieldSet()));
	}
	
}
