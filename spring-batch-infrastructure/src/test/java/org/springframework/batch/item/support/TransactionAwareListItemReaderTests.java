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

package org.springframework.batch.item.support;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class TransactionAwareListItemReaderTests {

	private ListItemReader<String> reader;

	@BeforeEach
	protected void setUp() throws Exception {
		reader = new ListItemReader<>(
				TransactionAwareProxyFactory.createTransactionalList(Arrays.asList("a", "b", "c")));
	}

	@Test
	public void testNext() throws Exception {
		assertEquals("a", reader.read());
		assertEquals("b", reader.read());
		assertEquals("c", reader.read());
		assertEquals(null, reader.read());
	}

	@Test
	public void testCommit() throws Exception {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		final List<Object> taken = new ArrayList<>();
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					taken.add(reader.read());
					return null;
				}
			});
		}
		catch (RuntimeException e) {
			fail("Unexpected RuntimeException");
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(1, taken.size());
		assertEquals("a", taken.get(0));
		taken.clear();
		Object next = reader.read();
		while (next != null) {
			taken.add(next);
			next = reader.read();
		}
		// System.err.println(taken);
		assertFalse(taken.contains("a"));
	}

	@Test
	public void testTransactionalExhausted() throws Exception {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		final List<Object> taken = new ArrayList<>();
		new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction(TransactionStatus status) {
				Object next = reader.read();
				while (next != null) {
					taken.add(next);
					next = reader.read();
				}
				return null;
			}
		});
		assertEquals(3, taken.size());
		assertEquals("a", taken.get(0));
	}

	@Test
	public void testRollback() throws Exception {
		PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
		final List<Object> taken = new ArrayList<>();
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback<Void>() {
				@Override
				public Void doInTransaction(TransactionStatus status) {
					taken.add(reader.read());
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(1, taken.size());
		assertEquals("a", taken.get(0));
		taken.clear();
		Object next = reader.read();
		while (next != null) {
			taken.add(next);
			next = reader.read();
		}
		System.err.println(taken);
		assertTrue(taken.contains("a"));
	}

}
