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
package org.springframework.batch.io.support;

import org.springframework.batch.io.ItemWriter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>
 * Abstract class that abstracts away transaction handling from input and
 * output. Any {@link ItemReader} or {@link ItemWriter} that wants to be
 * notified of transaction events to maintain the contract that all calls to
 * read or write can extend this base class to ensure that correct ordering is
 * maintained regardless of rollbacks.
 * </p>
 * 
 * <p>
 * This class is primarily useful because it allows its subclasses to implement
 * a single method to be notified of a commit or rollback, rather than having an
 * inner class that implements {@link TransactionSyncrhonization} and likely
 * calls another method with similar semantics as commit and rollback.
 * </p>
 * 
 * <p>
 * It should be noted that this implementation will only register for
 * synchronization if a call to registerSynchronization() has been made. This is
 * less than ideal, however, it is the best solution until {@link StepScope} is
 * modified to handle registering synchronizations in a scoped manner.
 * Otherwise, registering at instantiation or initialization (such as via the
 * Spring {@link InitializingBean} interface) would cause commits to be called
 * on input sources for all steps, rather than the currently running step.
 * </p>
 * 
 * @author Lucas Ward
 * @since 1.0
 * @see TransactionSynchronization
 * @see TransactionSynchronizationManager
 */
public abstract class AbstractTransactionalIoSource {

	private final TransactionSynchronization synchronization = new AbstractTransactionalIoSourceTransactionSynchronization();

	/**
	 * Register for Synchronization. This method is left protected because
	 * clients of this class should not be registering for synchronization, but
	 * rather only subclasses, at the appropriate time, i.e. when they are not
	 * initialized.
	 */
	protected void registerSynchronization() {
		BatchTransactionSynchronizationManager
				.registerSynchronization(synchronization);
	}

	/*
	 * Called when a transaction has been committed.
	 * 
	 * @see TransactionSynchronization#afterCompletion
	 */
	protected abstract void transactionCommitted();

	/*
	 * Called when a transaction has been rolled back.
	 * 
	 * @see TransactionSynchronization#afterCompletion
	 */
	protected abstract void transactionRolledBack();

	/**
	 * Encapsulates transaction events handling.
	 */
	private class AbstractTransactionalIoSourceTransactionSynchronization extends
			TransactionSynchronizationAdapter {
		public void afterCompletion(int status) {
			if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				transactionRolledBack();
			} else if (status == TransactionSynchronization.STATUS_COMMITTED) {
				transactionCommitted();
			}
		}
	}
}
