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
package org.springframework.batch.sample.support;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;
import org.springframework.batch.item.validator.ValidationException;

/**
 * @author Dave Syer
 *
 */
public class ItemTrackingItemWriterTests {
	
	private ItemTrackingItemWriter<String> writer = new ItemTrackingItemWriter<String>();

	/**
	 * Test method for {@link org.springframework.batch.sample.support.ItemTrackingItemWriter#write(java.util.List)}.
	 * @throws Exception 
	 */
	@Test
	public void testWrite() throws Exception {
		assertEquals(0, writer.getItems().size());
		writer.write(Arrays.asList("a", "b", "c"));
		assertEquals(3, writer.getItems().size());
	}

	@Test
	public void testValidationFailure() throws Exception {
		writer.setValidationFailure(2);
		try {
			writer.write(Arrays.asList("a", "b", "c"));
			fail("Expected ValidationException");
		}
		catch (ValidationException e) {
			// expected
		}
		// the failed item is removed
		assertEquals(2, writer.getItems().size());
		writer.write(Arrays.asList("a", "e", "c"));
		assertEquals(5, writer.getItems().size());
		try {
			writer.write(Arrays.asList("f", "b", "g"));
			fail("Expected ValidationException");
		}
		catch (ValidationException e) {
			// expected
		}
		// barf immediately if a failure is detected
		assertEquals(5, writer.getItems().size());
	}

}
