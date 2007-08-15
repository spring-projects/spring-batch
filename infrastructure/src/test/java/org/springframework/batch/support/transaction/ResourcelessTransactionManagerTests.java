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

import junit.framework.TestCase;

import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

public class ResourcelessTransactionManagerTests extends TestCase {

	ResourcelessTransactionManager transactionManager = new ResourcelessTransactionManager();

	int txStatus = Integer.MIN_VALUE;

	public void testCommit() throws Exception {
		new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
			public Object doInTransaction(TransactionStatus status) {
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
					public void afterCompletion(int status) {
						super.afterCompletion(status);
						txStatus = status;
					}
				});
				return null;
			}
		});
		assertEquals(TransactionSynchronization.STATUS_COMMITTED, txStatus);
	}

	public void testRollback() throws Exception {
		try {
			new TransactionTemplate(transactionManager).execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
						public void afterCompletion(int status) {
							super.afterCompletion(status);
							txStatus = status;
						}
					});
					throw new RuntimeException("Rollback!");
				}
			});
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("Rollback!", e.getMessage());
		}
		assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, txStatus);
	}
}
