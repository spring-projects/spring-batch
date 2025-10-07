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

package org.springframework.batch.infrastructure.item.support;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.support.ListItemReader;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.infrastructure.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionAwareListItemReaderTests {

	private final ListItemReader<String> reader = new ListItemReader<>(
			TransactionAwareProxyFactory.createTransactionalList(List.of("a", "b", "c")));

	@Test
	void testNext() {
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertEquals("c", reader.read());
		assertNull(reader.read());
	}

	@Test
	void testCommit() {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		final List<Object> taken = new ArrayList<>();
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			taken.add(reader.read());
			return null;
		});
		assertEquals(1, taken.size());
		assertEquals("a", taken.get(0));
		taken.clear();
		Object next = reader.read();
		while (next != null) {
			taken.add(next);
			next = reader.read();
		}
		assertFalse(taken.contains("a"));
	}

	@Test
	void testTransactionalExhausted() {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		final List<Object> taken = new ArrayList<>();
		new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
			Object next = reader.read();
			while (next != null) {
				taken.add(next);
				next = reader.read();
			}
			return null;
		});
		assertEquals(3, taken.size());
		assertEquals("a", taken.get(0));
	}

	@Test
	void testRollback() {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		final List<Object> taken = new ArrayList<>();
		Exception exception = assertThrows(RuntimeException.class,
				() -> new TransactionTemplate(transactionManager).execute((TransactionCallback<Void>) status -> {
					taken.add(reader.read());
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(1, taken.size());
		assertEquals("a", taken.get(0));
		taken.clear();
		Object next = reader.read();
		while (next != null) {
			taken.add(next);
			next = reader.read();
		}
		assertTrue(taken.contains("a"));
	}

}
