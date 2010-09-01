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

import java.util.Stack;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ResourcelessTransactionManager extends AbstractPlatformTransactionManager {

	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		((ResourcelessTransaction) transaction).begin();
	}

	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		logger.debug("Committing resourceless transaction on [" + status.getTransaction() + "]");
	}

	protected Object doGetTransaction() throws TransactionException {
		Object transaction = new ResourcelessTransaction();
		Stack<Object> resources;
		if (!TransactionSynchronizationManager.hasResource(this)) {
			resources = new Stack<Object>();
			TransactionSynchronizationManager.bindResource(this, resources);
		}
		else {
			@SuppressWarnings("unchecked")
			Stack<Object> stack = (Stack<Object>) TransactionSynchronizationManager.getResource(this);
			resources = stack;
		}
		resources.push(transaction);
		return transaction;
	}

	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		logger.debug("Rolling back resourceless transaction on [" + status.getTransaction() + "]");
	}

	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.hasResource(this)) {
			@SuppressWarnings("unchecked")
			Stack<Object> stack = (Stack<Object>) TransactionSynchronizationManager.getResource(this);
			return stack.size() > 1;
		}
		return ((ResourcelessTransaction) transaction).isActive();
	}

	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
	}

	protected void doCleanupAfterCompletion(Object transaction) {
		@SuppressWarnings("unchecked")
		Stack<Object> list = (Stack<Object>) TransactionSynchronizationManager.getResource(this);
		Stack<Object> resources = list;
		resources.clear();
		TransactionSynchronizationManager.unbindResource(this);
		((ResourcelessTransaction) transaction).clear();
	}

	private static class ResourcelessTransaction {

		private boolean active = false;

		public boolean isActive() {
			return active;
		}

		public void begin() {
			active = true;
		}

		public void clear() {
			active = false;
		}

	}

}
