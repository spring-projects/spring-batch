/*
 * Copyright 2006-2024 the original author or authors.
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
package org.springframework.batch.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;

import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @author Seokmun Heo
 */
class ExecutionContextTests {

	private final ExecutionContext context = new ExecutionContext();

	@Test
	void testNormalUsage() {

		context.putString("1", "testString1");
		context.putString("2", "testString2");
		context.putLong("3", 3);
		context.putDouble("4", 4.4);
		context.putInt("5", 5);

		assertEquals("testString1", context.getString("1"));
		assertEquals("testString2", context.getString("2"));
		assertEquals("defaultString", context.getString("55", "defaultString"));
		assertEquals(4.4, context.getDouble("4"), 0);
		assertEquals(5.5, context.getDouble("55", 5.5), 0);
		assertEquals(3, context.getLong("3"));
		assertEquals(5, context.getLong("55", 5));
		assertEquals(5, context.getInt("5"));
		assertEquals(6, context.getInt("55", 6));
	}

	@Test
	void testInvalidCast() {
		context.putLong("1", 1);
		assertThrows(ClassCastException.class, () -> context.getDouble("1"));
	}

	@Test
	void testIsEmpty() {
		assertTrue(context.isEmpty());
		context.putString("1", "test");
		assertFalse(context.isEmpty());
	}

	@Test
	void testDirtyFlag() {
		assertFalse(context.isDirty());
		context.putString("1", "test");
		assertTrue(context.isDirty());
		context.clearDirtyFlag();
		assertFalse(context.isDirty());
	}

	@Test
	void testNotDirtyWithDuplicate() {
		context.putString("1", "test");
		assertTrue(context.isDirty());
		context.clearDirtyFlag();
		context.putString("1", "test");
		assertFalse(context.isDirty());
	}

	@Test
	void testDirtyWithRemoveMissing() {
		context.putString("1", "test");
		assertTrue(context.isDirty());
		context.putString("1", null); // remove an item that was present
		assertTrue(context.isDirty());

		context.clearDirtyFlag();
		context.putString("1", null); // remove a non-existent item
		assertFalse(context.isDirty());
	}

	@Test
	void testContains() {
		context.putString("1", "testString");
		assertTrue(context.containsKey("1"));
		assertTrue(context.containsValue("testString"));
	}

	@Test
	void testEquals() {
		context.putString("1", "testString");
		ExecutionContext tempContext = new ExecutionContext();
		assertNotEquals(tempContext, context);
		tempContext.putString("1", "testString");
		assertEquals(tempContext, context);
	}

	/**
	 * Putting null value is equivalent to removing the entry for the given key.
	 */
	@Test
	void testPutNull() {
		context.put("1", null);
		assertNull(context.get("1"));
		assertFalse(context.containsKey("1"));
	}

	@Test
	void testGetNull() {
		assertNull(context.get("does not exist"));
	}

	@Test
	void testSerialization() {

		TestSerializable s = new TestSerializable();
		s.value = 7;

		context.putString("1", "testString1");
		context.putString("2", "testString2");
		context.putLong("3", 3);
		context.putDouble("4", 4.4);
		context.put("5", s);
		context.putInt("6", 6);

		ExecutionContext clone = SerializationUtils.clone(context);

		assertEquals(context, clone);
		assertEquals(7, ((TestSerializable) clone.get("5")).value);
	}

	@Test
	void testCopyConstructor() {
		ExecutionContext context = new ExecutionContext();
		context.put("foo", "bar");
		ExecutionContext copy = new ExecutionContext(context);
		assertEquals(copy, context);
	}

	@Test
	void testCopyConstructorNullInput() {
		ExecutionContext context = new ExecutionContext((ExecutionContext) null);
		assertTrue(context.isEmpty());
	}

	@Test
	void testDirtyWithDuplicate() {
		ExecutionContext context = new ExecutionContext();
		context.put("1", "testString1");
		assertTrue(context.isDirty());
		context.put("1", "testString1"); // put the same value
		assertTrue(context.isDirty());
	}

	/**
	 * Value object for testing serialization
	 */
	private static class TestSerializable implements Serializable {

		int value;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + value;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			TestSerializable other = (TestSerializable) obj;
			if (value != other.value) {
				return false;
			}
			return true;
		}

	}

}
