/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 *
 */
class EntityTests {

	private Entity entity = new Entity(11L);

	@Test
	void testHashCode() {
		assertEquals(entity.hashCode(), new Entity(entity.getId()).hashCode());
	}

	@Test
	void testHashCodeNullId() {
		int withoutNull = entity.hashCode();
		entity.setId(null);
		int withNull = entity.hashCode();
		assertTrue(withoutNull != withNull);
	}

	@Test
	void testGetVersion() {
		assertNull(entity.getVersion());
	}

	@Test
	void testIncrementVersion() {
		entity.incrementVersion();
		assertEquals(Integer.valueOf(0), entity.getVersion());
	}

	@Test
	void testIncrementVersionTwice() {
		entity.incrementVersion();
		entity.incrementVersion();
		assertEquals(Integer.valueOf(1), entity.getVersion());
	}

	@Test
	void testToString() {
		Entity job = new Entity();
		assertTrue(job.toString().contains("id=null"));
	}

	@Test
	void testEqualsSelf() {
		assertEquals(entity, entity);
	}

	@Test
	void testEqualsSelfWithNullId() {
		entity = new Entity(null);
		assertEquals(entity, entity);
	}

	@Test
	void testEqualsEntityWithNullId() {
		entity = new Entity(null);
		assertNotSame(entity, new Entity(null));
	}

	@Test
	void testEqualsEntity() {
		assertEquals(entity, new Entity(entity.getId()));
	}

	@Test
	void testEqualsEntityWrongId() {
		assertNotEquals(entity, new Entity());
	}

	@Test
	void testEqualsObject() {
		assertNotEquals(entity, new Object());
	}

	@Test
	void testEqualsNull() {
		assertNotEquals(null, entity);
	}

}
