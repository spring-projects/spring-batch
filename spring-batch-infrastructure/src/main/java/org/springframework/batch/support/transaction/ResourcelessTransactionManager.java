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

import java.util.ArrayList;
import java.util.List;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@SuppressWarnings("serial")
public class ResourcelessTransactionManager extends AbstractPlatformTransactionManager {

    @Override
	protected void doBegin(Object transaction, TransactionDefinition definition) throws TransactionException {
		((ResourcelessTransaction) transaction).begin();
	}

    @Override
	protected void doCommit(DefaultTransactionStatus status) throws TransactionException {
		if (logger.isDebugEnabled()) {
			logger.debug("Committing resourceless transaction on [" + status.getTransaction() + "]");
		}
	}

    @Override
	protected Object doGetTransaction() throws TransactionException {
		Object transaction = new ResourcelessTransaction();
		List<Object> resources;
		if (!TransactionSynchronizationManager.hasResource(this)) {
			resources = new ArrayList<>();
			TransactionSynchronizationManager.bindResource(this, resources);
		}
		else {
			@SuppressWarnings("unchecked")
			List<Object> stack = (List<Object>) TransactionSynchronizationManager.getResource(this);
			resources = stack;
		}
		resources.add(transaction);
		return transaction;
	}

    @Override
	protected void doRollback(DefaultTransactionStatus status) throws TransactionException {
		if (logger.isDebugEnabled()) {
			logger.debug("Rolling back resourceless transaction on [" + status.getTransaction() + "]");
		}
	}

    @Override
	protected boolean isExistingTransaction(Object transaction) throws TransactionException {
		if (TransactionSynchronizationManager.hasResource(this)) {
			List<?> stack = (List<?>) TransactionSynchronizationManager.getResource(this);
			return stack.size() > 1;
		}
		return ((ResourcelessTransaction) transaction).isActive();
	}

    @Override
	protected void doSetRollbackOnly(DefaultTransactionStatus status) throws TransactionException {
	}

    @Override
	protected void doCleanupAfterCompletion(Object transaction) {
		List<?> resources = (List<?>) TransactionSynchronizationManager.getResource(this);
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
