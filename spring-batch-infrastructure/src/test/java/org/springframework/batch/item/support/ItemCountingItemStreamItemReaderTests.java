/*
 * Copyright 2006-2019 the original author or authors.
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
package org.springframework.batch.item.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * 
 */
public class ItemCountingItemStreamItemReaderTests {

	private ItemCountingItemStreamItemReader reader = new ItemCountingItemStreamItemReader();
	
	@Before
	public void setUp() {
		reader.setName("foo");
	}

	@Test
	public void testJumpToItem() throws Exception {
		reader.jumpToItem(2);
		assertEquals(2, reader.getCurrentItemCount());
		reader.read();
		assertEquals(3, reader.getCurrentItemCount());
	}

	@Test
	public void testGetCurrentItemCount() throws Exception {
		assertEquals(0, reader.getCurrentItemCount());
		reader.read();
		assertEquals(1, reader.getCurrentItemCount());
	}

	@Test
	public void testClose() {
		reader.close();
		assertTrue(reader.closeCalled);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testOpenWithoutName() {
		reader = new ItemCountingItemStreamItemReader();
		reader.open(new ExecutionContext());
		assertFalse(reader.openCalled);
	}

	@Test
	public void testOpen() {
		reader.open(new ExecutionContext());
		assertTrue(reader.openCalled);
	}

	@Test
	public void testReadToEnd() throws Exception {
		reader.read();
		reader.read();
		reader.read();
		assertNull(reader.read());
	}

	@Test
	public void testUpdate() throws Exception {
		reader.read();
		ExecutionContext context = new ExecutionContext();
		reader.update(context);
		assertEquals(1, context.size());
		assertEquals(1, context.getInt("foo.read.count"));
	}

	@Test
	public void testSetName() throws Exception {
		reader.setName("bar");
		reader.read();
		ExecutionContext context = new ExecutionContext();
		reader.update(context);
		assertEquals(1, context.getInt("bar.read.count"));
	}

	@Test
	public void testSetSaveState() throws Exception {
		reader.read();
		ExecutionContext context = new ExecutionContext();
		reader.update(context);
		assertEquals(1, context.size());
	}

	@Test
	public void testReadToEndWithMax() throws Exception {
		ExecutionContext context = new ExecutionContext();
		context.putInt("foo.read.count.max", 1);
		reader.open(context);
		reader.read();
		assertNull(reader.read());
	}

	@Test
	public void testUpdateWithMax() throws Exception {
		ExecutionContext context = new ExecutionContext();
		context.putInt("foo.read.count.max", 1);
		reader.open(context);
		reader.update(context);
		assertEquals(2, context.size());
	}

	private static class ItemCountingItemStreamItemReader extends AbstractItemCountingItemStreamItemReader<String> {

		private boolean closeCalled = false;

		private boolean openCalled = false;

		private Iterator<String> items = Arrays.asList("a", "b", "c").iterator();

		@Override
		protected void doClose() throws Exception {
			closeCalled = true;
		}

		@Override
		protected void doOpen() throws Exception {
			openCalled = true;
		}

		@Nullable
		@Override
		protected String doRead() throws Exception {
			if (!items.hasNext()) {
				return null;
			}
			return items.next();
		}

	}

}
