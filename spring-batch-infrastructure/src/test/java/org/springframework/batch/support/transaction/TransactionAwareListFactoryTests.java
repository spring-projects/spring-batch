/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.support.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionAwareListFactoryTests {

	private TransactionTemplate transactionTemplate = new TransactionTemplate(new ResourcelessTransactionManager());

	private List<String> list;

	@Before
	public void setUp() throws Exception {
		list = TransactionAwareProxyFactory.createTransactionalList(Arrays.asList("foo", "bar", "spam"));
	}

	@Test
	public void testAdd() {
		assertEquals(3, list.size());
		list.add("bucket");
		assertTrue(list.contains("bucket"));
	}

	@Test
	public void testRemove() {
		assertEquals(3, list.size());
		assertTrue(list.contains("spam"));
		list.remove("spam");
		assertFalse(list.contains("spam"));
	}

	@Test
	public void testClear() {
		assertEquals(3, list.size());
		list.clear();
		assertEquals(0, list.size());
	}

	@Test
	public void testTransactionalAdd() throws Exception {
		transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
			public Void doInTransaction(TransactionStatus status) {
				testAdd();
				return null;
			}
		});
		assertEquals(4, list.size());
	}

	@Test
	public void testTransactionalRemove() throws Exception {
		transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
			public Void doInTransaction(TransactionStatus status) {
				testRemove();
				return null;
			}
		});
		assertEquals(2, list.size());
	}

	@Test
	public void testTransactionalClear() throws Exception {
		transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
			public Void doInTransaction(TransactionStatus status) {
				testClear();
				return null;
			}
		});
		assertEquals(0, list.size());
	}

	@Test
	public void testTransactionalAddWithRollback() throws Exception {
		try {
			transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
				public Void doInTransaction(TransactionStatus status) {
					testAdd();
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(3, list.size());
	}

	@Test
	public void testTransactionalRemoveWithRollback() throws Exception {
		try {
			transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
				public Void doInTransaction(TransactionStatus status) {
					testRemove();
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(3, list.size());
	}

	@Test
	public void testTransactionalClearWithRollback() throws Exception {
		try {
			transactionTemplate.execute(new TransactionCallback<Void>() {
                @Override
				public Void doInTransaction(TransactionStatus status) {
					testClear();
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(3, list.size());
	}
}
