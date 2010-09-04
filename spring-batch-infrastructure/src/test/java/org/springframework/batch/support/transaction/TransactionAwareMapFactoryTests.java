/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.support.transaction;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

public class TransactionAwareMapFactoryTests extends TestCase {

	TransactionTemplate transactionTemplate = new TransactionTemplate(new ResourcelessTransactionManager());

	Map<String, String> map;

	protected void setUp() throws Exception {
		Map<String, String> seed = new HashMap<String, String>();
		seed.put("foo", "oof");
		seed.put("bar", "bar");
		seed.put("spam", "maps");
		map = TransactionAwareProxyFactory.createTransactionalMap(seed);
	}

	public void testAdd() {
		assertEquals(3, map.size());
		map.put("bucket", "crap");
		assertTrue(map.keySet().contains("bucket"));
	}

	public void testEmpty() {
		assertEquals(3, map.size());
		map.put("bucket", "crap");
		assertFalse(map.isEmpty());
	}

	public void testValues() {
		assertEquals(3, map.size());
		map.put("bucket", "crap");
		assertEquals(4, map.keySet().size());
	}

	public void testRemove() {
		assertEquals(3, map.size());
		assertTrue(map.keySet().contains("spam"));
		map.remove("spam");
		assertFalse(map.keySet().contains("spam"));
	}

	public void testClear() {
		assertEquals(3, map.size());
		map.clear();
		assertEquals(0, map.size());
	}

	public void testTransactionalAdd() throws Exception {
		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				testAdd();
				return null;
			}
		});
		assertEquals(4, map.size());
	}

	public void testTransactionalEmpty() throws Exception {
		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				testEmpty();
				return null;
			}
		});
		assertEquals(4, map.size());
	}

	public void testTransactionalValues() throws Exception {
		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				testValues();
				return null;
			}
		});
		assertEquals(4, map.size());
	}

	public void testTransactionalRemove() throws Exception {
		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				testRemove();
				return null;
			}
		});
		assertEquals(2, map.size());
	}

	public void testTransactionalClear() throws Exception {
		transactionTemplate.execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				testClear();
				return null;
			}
		});
		assertEquals(0, map.size());
	}

	public void testTransactionalAddWithRollback() throws Exception {
		try {
			transactionTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					testAdd();
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(3, map.size());
	}

	public void testTransactionalRemoveWithRollback() throws Exception {
		try {
			transactionTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					testRemove();
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(3, map.size());
	}

	public void testTransactionalClearWithRollback() throws Exception {
		try {
			transactionTemplate.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					testClear();
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(3, map.size());
	}
}
