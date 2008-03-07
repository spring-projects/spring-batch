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
package org.springframework.batch.io.file.transform;

import junit.framework.TestCase;

import org.springframework.batch.io.file.mapping.FieldSet;

/**
 * @author Dave Syer
 *
 */
public class LineAggregatorItemTransformerTests extends TestCase {
	
	private LineAggregatorItemTransformer transformer = new LineAggregatorItemTransformer();

	/**
	 * Test method for {@link org.springframework.batch.io.file.transform.LineAggregatorItemTransformer#setAggregator(org.springframework.batch.io.file.transform.LineAggregator)}.
	 * @throws Exception 
	 */
	public void testSetAggregator() throws Exception {
		transformer.setAggregator(new LineAggregator() {
			public String aggregate(FieldSet fieldSet) {
				return "foo";
			}
		});
		String value = (String) transformer.transform(new String[] {"a", "b"});
		assertEquals("foo", value);
	}

	/**
	 * Test method for {@link org.springframework.batch.io.file.transform.LineAggregatorItemTransformer#transform(java.lang.Object)}.
	 * @throws Exception 
	 */
	public void testTransform() throws Exception {
		String value = (String) transformer.transform(new String[] {"a", "b"});
		assertTrue("Wrong value: "+value, value.startsWith("a,b"));
	}

	/**
	 * Test method for {@link org.springframework.batch.io.file.transform.LineAggregatorItemTransformer#transform(java.lang.Object)}.
	 * @throws Exception 
	 */
	public void testTransformWrongType() throws Exception {
		try {
			transformer.transform("foo");
			fail("Expected ConversionException");
		} catch (ConversionException e) {
			// Expected
		}

	}
}
