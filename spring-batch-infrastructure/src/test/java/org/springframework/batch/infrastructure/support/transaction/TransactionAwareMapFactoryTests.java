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

package org.springframework.batch.infrastructure.support.transaction;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.infrastructure.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionAwareMapFactoryTests {

	private final TransactionTemplate transactionTemplate = new TransactionTemplate(
			new ResourcelessTransactionManager());

	private Map<String, String> map;

	@BeforeEach
	void setUp() {
		Map<String, String> seed = new HashMap<>();
		seed.put("foo", "oof");
		seed.put("bar", "bar");
		seed.put("spam", "maps");
		map = TransactionAwareProxyFactory.createTransactionalMap(seed);
	}

	@Test
	void testAdd() {
		assertEquals(3, map.size());
		map.put("bucket", "crap");
		assertTrue(map.containsKey("bucket"));
	}

	@Test
	void testEmpty() {
		assertEquals(3, map.size());
		map.put("bucket", "crap");
		assertFalse(map.isEmpty());
	}

	@Test
	void testValues() {
		assertEquals(3, map.size());
		map.put("bucket", "crap");
		assertEquals(4, map.keySet().size());
	}

	@Test
	void testRemove() {
		assertEquals(3, map.size());
		assertTrue(map.containsKey("spam"));
		map.remove("spam");
		assertFalse(map.containsKey("spam"));
	}

	@Test
	void testClear() {
		assertEquals(3, map.size());
		map.clear();
		assertEquals(0, map.size());
	}

	@Test
	void testTransactionalAdd() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testAdd();
			return null;
		});
		assertEquals(4, map.size());
	}

	@Test
	void testTransactionalEmpty() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testEmpty();
			return null;
		});
		assertEquals(4, map.size());
	}

	@Test
	void testTransactionalValues() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testValues();
			return null;
		});
		assertEquals(4, map.size());
	}

	@Test
	void testTransactionalRemove() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testRemove();
			return null;
		});
		assertEquals(2, map.size());
	}

	@Test
	void testTransactionalClear() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testClear();
			return null;
		});
		assertEquals(0, map.size());
	}

	@Test
	void testTransactionalAddWithRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> transactionTemplate.execute((TransactionCallback<Void>) status -> {
					testAdd();
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(3, map.size());
	}

	@Test
	void testTransactionalRemoveWithRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> transactionTemplate.execute((TransactionCallback<Void>) status -> {
					testRemove();
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(3, map.size());
	}

	@Test
	void testTransactionalClearWithRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> transactionTemplate.execute((TransactionCallback<Void>) status -> {
					testClear();
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(3, map.size());
	}

}
