/*
 * Copyright 2006-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Iterator;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class ItemCountingItemStreamItemReaderTests {

	private ItemCountingItemStreamItemReader reader = new ItemCountingItemStreamItemReader();

	@BeforeEach
	void setUp() {
		reader.setName("foo");
	}

	@Test
	void testJumpToItem() throws Exception {
		reader.jumpToItem(2);
		assertEquals(2, reader.getCurrentItemCount());
		reader.read();
		assertEquals(3, reader.getCurrentItemCount());
	}

	@Test
	void testGetCurrentItemCount() throws Exception {
		assertEquals(0, reader.getCurrentItemCount());
		reader.read();
		assertEquals(1, reader.getCurrentItemCount());
	}

	@Test
	void testClose() {
		reader.close();
		assertTrue(reader.closeCalled);
	}

	@Test
	void testOpenWithoutName() {
		reader = new ItemCountingItemStreamItemReader();
		assertThrows(IllegalArgumentException.class, () -> reader.open(new ExecutionContext()));
	}

	@Test
	void testOpen() {
		reader.open(new ExecutionContext());
		assertTrue(reader.openCalled);
	}

	@Test
	void testReadToEnd() throws Exception {
		reader.read();
		reader.read();
		reader.read();
		assertNull(reader.read());
	}

	@Test
	void testUpdate() throws Exception {
		reader.read();
		ExecutionContext context = new ExecutionContext();
		reader.update(context);
		assertEquals(1, context.size());
		assertEquals(1, context.getInt("foo.read.count"));
	}

	@Test
	void testSetName() throws Exception {
		reader.setName("bar");
		reader.read();
		ExecutionContext context = new ExecutionContext();
		reader.update(context);
		assertEquals(1, context.getInt("bar.read.count"));
	}

	@Test
	void testSetSaveState() throws Exception {
		reader.read();
		ExecutionContext context = new ExecutionContext();
		reader.update(context);
		assertEquals(1, context.size());
	}

	@Test
	void testReadToEndWithMax() throws Exception {
		ExecutionContext context = new ExecutionContext();
		context.putInt("foo.read.count.max", 1);
		reader.open(context);
		reader.read();
		assertNull(reader.read());
	}

	@Test
	void testUpdateWithMax() {
		ExecutionContext context = new ExecutionContext();
		context.putInt("foo.read.count.max", 1);
		reader.open(context);
		reader.update(context);
		assertEquals(2, context.size());
	}

	private static class ItemCountingItemStreamItemReader extends AbstractItemCountingItemStreamItemReader<String> {

		private boolean closeCalled = false;

		private boolean openCalled = false;

		private final Iterator<String> items = Arrays.asList("a", "b", "c").iterator();

		@Override
		protected void doClose() throws Exception {
			closeCalled = true;
		}

		@Override
		protected void doOpen() throws Exception {
			openCalled = true;
		}

		@Override
		protected @Nullable String doRead() throws Exception {
			if (!items.hasNext()) {
				return null;
			}
			return items.next();
		}

	}

}
