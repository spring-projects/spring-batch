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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.infrastructure.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

class TransactionAwareListFactoryTests {

	private final TransactionTemplate transactionTemplate = new TransactionTemplate(
			new ResourcelessTransactionManager());

	private List<String> list;

	@BeforeEach
	void setUp() {
		list = TransactionAwareProxyFactory.createTransactionalList(Arrays.asList("foo", "bar", "spam"));
	}

	@Test
	void testAdd() {
		assertEquals(3, list.size());
		list.add("bucket");
		assertTrue(list.contains("bucket"));
	}

	@Test
	void testRemove() {
		assertEquals(3, list.size());
		assertTrue(list.contains("spam"));
		list.remove("spam");
		assertFalse(list.contains("spam"));
	}

	@Test
	void testClear() {
		assertEquals(3, list.size());
		list.clear();
		assertEquals(0, list.size());
	}

	@Test
	void testTransactionalAdd() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testAdd();
			return null;
		});
		assertEquals(4, list.size());
	}

	@Test
	void testTransactionalRemove() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testRemove();
			return null;
		});
		assertEquals(2, list.size());
	}

	@Test
	void testTransactionalClear() {
		transactionTemplate.execute((TransactionCallback<Void>) status -> {
			testClear();
			return null;
		});
		assertEquals(0, list.size());
	}

	@Test
	void testTransactionalAddWithRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> transactionTemplate.execute((TransactionCallback<Void>) status -> {
					testAdd();
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(3, list.size());
	}

	@Test
	void testTransactionalRemoveWithRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> transactionTemplate.execute((TransactionCallback<Void>) status -> {
					testRemove();
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(3, list.size());
	}

	@Test
	void testTransactionalClearWithRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> transactionTemplate.execute((TransactionCallback<Void>) status -> {
					testClear();
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(3, list.size());
	}

}
