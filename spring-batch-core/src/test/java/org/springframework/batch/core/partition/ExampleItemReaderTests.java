/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.batch.core.partition;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;

public class ExampleItemReaderTests {
	
	private ExampleItemReader reader = new ExampleItemReader();
	
	@Before
	@After
	public void ensureFailFlagUnset() {
		ExampleItemReader.fail = false;
	}

	@Test
	public void testRead() throws Exception {
		int count = 0;
		while (reader.read()!=null) {
			count++;
		}
		assertEquals(8, count);
	}

	@Test
	public void testOpen() throws Exception {
		ExecutionContext context = new ExecutionContext();
		for (int i=0; i<4; i++) {
			reader.read();
		}
		reader.update(context);
		reader.open(context);
		int count = 0;
		while (reader.read()!=null) {
			count++;
		}
		assertEquals(4, count);		
	}

	@Test
	public void testFailAndRestart() throws Exception {
		ExecutionContext context = new ExecutionContext();
		ExampleItemReader.fail = true;
		for (int i=0; i<4; i++) {
			reader.read();
			reader.update(context);
		}
		try {
			reader.read();
			reader.update(context);
			fail("Expected Exception");
		}
		catch (Exception e) {
			// expected
			assertEquals("Planned failure", e.getMessage());
		}
		assertFalse(ExampleItemReader.fail);
		reader.open(context);
		int count = 0;
		while (reader.read()!=null) {
			count++;
		}
		assertEquals(4, count);		
	}

}
