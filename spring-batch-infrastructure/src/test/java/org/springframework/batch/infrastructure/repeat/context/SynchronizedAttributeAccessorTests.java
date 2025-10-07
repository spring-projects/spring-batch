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

package org.springframework.batch.infrastructure.repeat.context;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.repeat.context.SynchronizedAttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SynchronizedAttributeAccessorTests {

	private final SynchronizedAttributeAccessor accessor = new SynchronizedAttributeAccessor();

	@Test
	void testHashCode() {
		SynchronizedAttributeAccessor another = new SynchronizedAttributeAccessor();
		accessor.setAttribute("foo", "bar");
		another.setAttribute("foo", "bar");
		assertEquals(accessor, another);
		assertEquals(accessor.hashCode(), another.hashCode(), "Object.hashCode() contract broken");
	}

	@Test
	void testToStringWithNoAttributes() {
		assertNotNull(accessor.toString());
	}

	@Test
	void testToStringWithAttributes() {
		accessor.setAttribute("foo", "bar");
		accessor.setAttribute("spam", "bucket");
		assertNotNull(accessor.toString());
	}

	@Test
	void testAttributeNames() {
		accessor.setAttribute("foo", "bar");
		accessor.setAttribute("spam", "bucket");
		List<String> list = Arrays.asList(accessor.attributeNames());
		assertEquals(2, list.size());
		assertTrue(list.contains("foo"));
	}

	@Test
	void testEqualsSameType() {
		SynchronizedAttributeAccessor another = new SynchronizedAttributeAccessor();
		accessor.setAttribute("foo", "bar");
		another.setAttribute("foo", "bar");
		assertEquals(accessor, another);
	}

	@Test
	void testEqualsSelf() {
		accessor.setAttribute("foo", "bar");
		assertEquals(accessor, accessor);
	}

	@Test
	void testEqualsWrongType() {
		accessor.setAttribute("foo", "bar");
		Map<String, String> another = Collections.singletonMap("foo", "bar");
		// Accessor and another are instances of unrelated classes, they should
		// never be equal...
		assertNotEquals(accessor, another);
	}

	@Test
	void testEqualsSupport() {
		@SuppressWarnings("serial")
		AttributeAccessorSupport another = new AttributeAccessorSupport() {
		};
		accessor.setAttribute("foo", "bar");
		another.setAttribute("foo", "bar");
		assertEquals(accessor, another);
	}

	@Test
	void testGetAttribute() {
		accessor.setAttribute("foo", "bar");
		assertEquals("bar", accessor.getAttribute("foo"));
	}

	@Test
	void testSetAttributeIfAbsentWhenAlreadyPresent() {
		accessor.setAttribute("foo", "bar");
		assertEquals("bar", accessor.setAttributeIfAbsent("foo", "spam"));
	}

	@Test
	void testSetAttributeIfAbsentWhenNotAlreadyPresent() {
		assertNull(accessor.setAttributeIfAbsent("foo", "bar"));
		assertEquals("bar", accessor.getAttribute("foo"));
	}

	@Test
	void testHasAttribute() {
		accessor.setAttribute("foo", "bar");
		assertTrue(accessor.hasAttribute("foo"));
	}

	@Test
	void testRemoveAttribute() {
		accessor.setAttribute("foo", "bar");
		assertEquals("bar", accessor.getAttribute("foo"));
		accessor.removeAttribute("foo");
		assertNull(accessor.getAttribute("foo"));
	}

}
