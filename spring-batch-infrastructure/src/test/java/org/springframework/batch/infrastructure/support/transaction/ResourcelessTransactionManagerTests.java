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

package org.springframework.batch.infrastructure.support.transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

class ResourcelessTransactionManagerTests {

	private final ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

	private int txStatus = Integer.MIN_VALUE;

	private int count = 0;

	@Test
	void testCommit() {
		new TransactionTemplate(transactionManager).execute(status -> {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					txStatus = status;
				}
			});
			return null;
		});
		assertEquals(TransactionSynchronization.STATUS_COMMITTED, txStatus);
	}

	@Test
	void testCommitTwice() {
		testCommit();
		txStatus = -1;
		new TransactionTemplate(transactionManager).execute(status -> {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					txStatus = status;
				}
			});
			return null;
		});
		assertEquals(TransactionSynchronization.STATUS_COMMITTED, txStatus);
	}

	@Test
	void testCommitNested() {
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(outerStatus -> {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					txStatus = status;
					count++;
				}
			});
			transactionTemplate.execute(innerStatus -> {
				assertEquals(0, count);
				count++;
				return null;
			});
			assertEquals(1, count);
			return null;
		});
		assertEquals(TransactionSynchronization.STATUS_COMMITTED, txStatus);
		assertEquals(2, count);
	}

	@Test
	void testCommitNestedTwice() {
		testCommitNested();
		count = 0;
		txStatus = -1;
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		transactionTemplate.execute(outerStatus -> {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					txStatus = status;
					count++;
				}
			});
			transactionTemplate.execute(innerStatus -> {
				assertEquals(0, count);
				count++;
				return null;
			});
			assertEquals(1, count);
			return null;
		});
		assertEquals(TransactionSynchronization.STATUS_COMMITTED, txStatus);
		assertEquals(2, count);
	}

	@Test
	void testRollback() {
		Exception exception = assertThrows(RuntimeException.class,
				() -> new TransactionTemplate(transactionManager).execute(status -> {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
						@Override
						public void afterCompletion(int status) {
							txStatus = status;
						}
					});
					throw new RuntimeException("Rollback!");
				}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, txStatus);
	}

	@Test
	void testRollbackNestedInner() {
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Exception exception = assertThrows(RuntimeException.class, () -> transactionTemplate.execute(outerStatus -> {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					txStatus = status;
					count++;
				}
			});
			transactionTemplate.execute(innerStatus -> {
				assertEquals(0, count);
				count++;
				throw new RuntimeException("Rollback!");
			});
			assertEquals(1, count);
			return null;
		}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, txStatus);
		assertEquals(2, count);
	}

	@Test
	void testRollbackNestedOuter() {
		final TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
		Exception exception = assertThrows(RuntimeException.class, () -> transactionTemplate.execute(outerStatus -> {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					txStatus = status;
					count++;
				}
			});
			transactionTemplate.execute(innerStatus -> {
				assertEquals(0, count);
				count++;
				return null;
			});
			assertEquals(1, count);
			throw new RuntimeException("Rollback!");
		}));
		assertEquals("Rollback!", exception.getMessage());
		assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, txStatus);
		assertEquals(2, count);
	}

}
