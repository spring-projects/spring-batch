/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.batch.item;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Jimmy Praet
 */
public class ItemStreamSupportTests {
	
	private ItemStreamSupport itemStreamSupport;
	
	private ExecutionContext executionContext;
	
	@Before
	public void setUp() {
		itemStreamSupport = new TestItemStreamSupport();
		executionContext = new ExecutionContext();
	}
	
	@Test
	public void testGetExecutionContextKey() {
		Assert.assertEquals("TestItemStreamSupport.foo", itemStreamSupport.getExecutionContextKey("foo"));
	}
	
	@Test
	public void testOpen() {
		itemStreamSupport.open(executionContext);
		Assert.assertEquals(1, executionContext.size());
	}

	@Test
	public void testUpdateBeforeOpen() {
		itemStreamSupport.update(executionContext);
		Assert.assertEquals(0, executionContext.size());
	}

	@Test
	public void testUpdateAfterOpen() {
		itemStreamSupport.open(executionContext);
		itemStreamSupport.update(executionContext);
		Assert.assertEquals(1, executionContext.size());
	}
	
	@Test(expected=IllegalStateException.class)
	public void testExecutionContextKeyCollision() {
		ItemStreamSupport other = new TestItemStreamSupport();
		itemStreamSupport.open(executionContext);
		other.open(executionContext);
		itemStreamSupport.update(executionContext);
		other.update(executionContext);
	}

	@Test
	public void testExecutionContextNoKeyCollision() {
		ItemStreamSupport other = new TestItemStreamSupport();
		other.setName("unique_name");
		itemStreamSupport.open(executionContext);
		other.open(executionContext);
		itemStreamSupport.update(executionContext);
		other.update(executionContext);
	}

	private static final class TestItemStreamSupport extends ItemStreamSupport {
		
		public TestItemStreamSupport() {
			setName("TestItemStreamSupport");
		}
	}

}
