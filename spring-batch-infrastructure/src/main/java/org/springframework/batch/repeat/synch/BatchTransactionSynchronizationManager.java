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

package org.springframework.batch.repeat.synch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.repeat.RepeatContext;
import org.springframework.core.AttributeAccessor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>
 * Contains static methods for registering objects for transaction
 * synchronization. Because there are many non-standard inputs that need to be
 * aware of transactions, such as file input, this facade provides a hook into
 * Spring TransactionSyncrhonization. The spring class
 * TransactionSynchronizationManager has public static methods that are used by
 * the AbstractPlatformTransactionManager to ensure other resources are
 * synchronizaed with it's transaction. This means that any spring transaction
 * manager which extends the afore mentioned abstract class will be notified of
 * changes in a transaction. For more information on the type of transaction
 * events that can be handled, please see the TransactionSynchronization
 * interface.
 * </p>
 * 
 * <p>
 * Spring's intended use for the TransactionSyncrhonizationManager is that any
 * class that wishes can register for the current transaction only. When commit
 * or rollback is called, this list will be used to notify interested classes.
 * However, once a new transaction is obtained, this list will be cleared. This
 * is problematic for batch processing, since input templates need to always be
 * made aware of transaction events, without being forced to register every
 * time. To solve this issue, classes should register with the
 * BatchTransactionFacade, which will ensure that any time a new transaction is
 * obtained, they are re-registered with spring's transaction synchronization.
 * </p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see TransactionSynchronizationManager
 * @see RepeatSynchronizationManager
 * @see TransactionSynchronization
 */
public class BatchTransactionSynchronizationManager {

	/**
	 * The key in the context attributes for the list of synchronizations.
	 */
	private static final String SYNCHS_ATTR_KEY = BatchTransactionSynchronizationManager.class.getName()
			+ ".SYNCHRONIZATIONS";

	/**
	 * Static method to register synchronizations. A TransactionSyncrhonization
	 * object will be added to the internal list within a threadLocal. After
	 * ensuring that there is a reference for later re-synchronization, the
	 * object is added to spring's TransactionSynchronizationManager.
	 */
	public static void registerSynchronization(TransactionSynchronization synchronization) {
		List synchs = (List) getSynchronizations();

		if (!synchs.contains(synchronization)) {
			synchs.add(synchronization);
			if (TransactionSynchronizationManager.isSynchronizationActive()
					&& !TransactionSynchronizationManager.getSynchronizations().contains(synchronization)) {
				TransactionSynchronizationManager.registerSynchronization(synchronization);
			}
		}

	}

	/**
	 * The internal list of synchronizations is iterated, and each
	 * synchronization object is registered with the
	 * TransactionSynchronizationManager again. This is necessary because any
	 * call to PlatformTransactionManager.getTransaction() will result in a
	 * clearing of the synchronizationManager's list.
	 */
	public static void resynchronize() {
		List batchSynchs = (List) getSynchronizations();

		if (batchSynchs != null) {
			for (Iterator it = batchSynchs.iterator(); it.hasNext();) {
				TransactionSynchronization synchronization = (TransactionSynchronization) it.next();
				if (TransactionSynchronizationManager.isSynchronizationActive()
						&& !TransactionSynchronizationManager.getSynchronizations().contains(synchronization)) {
					TransactionSynchronizationManager.registerSynchronization(synchronization);
				}
			}
		}
	}

	/**
	 * Set the synchronizations list to null. Usually called when the step is
	 * complete, to ensure no issues when the next step is called within the
	 * same thread, which should only happen when running out of container. Does
	 * not throw an exception if there is no batch context.
	 */
	public static void clearSynchronizations() {
		AttributeAccessor context = getContext();
		if (context == null) {
			return; // Nothing to do
		}
		setSynchronizations(null);
	}

	/**
	 * Set the current synchronizations to the given list.
	 * @param synchs a list of {@link TransactionSynchronization} instances.
	 * @throws IllegalStateException if there is no batch context available.
	 */
	private static void setSynchronizations(List synchs) {
		AttributeAccessor context = getContext();
		if (context == null) {
			return;
		}
		context.setAttribute(SYNCHS_ATTR_KEY, synchs);
	}

	/**
	 * Get the current list of synchronizations if there is one.
	 * 
	 * @return a list of {@link TransactionSynchronization} instances or null.
	 * 
	 * @throws IllegalStateException if there is no batch context available.
	 */
	private static List getSynchronizations() {

		AttributeAccessor context = getContext();
		if (context == null) {
			// N.B. this returns a modifiable list on purpose - it is used
			// internally to set up the list if there is no context available
			// (useful in testing).
			return new ArrayList();
		}
		List synchs = (List) context.getAttribute(SYNCHS_ATTR_KEY);

		if (synchs == null) {
			synchs = new ArrayList();
		}

		setSynchronizations(synchs);

		return synchs;
	}

	/**
	 * @return the current context as an {@link AttributeAccessor}.
	 */
	private static AttributeAccessor getContext() {
		RepeatContext context = RepeatSynchronizationManager.getContext();
		return getSynchContext(context);
	}

	/**
	 * Locate the context that will contain the synchronisations by walking up
	 * the context hierarchy until the synchronisations are found or the top is
	 * reached.
	 * 
	 * @param context
	 * @return
	 */
	private static AttributeAccessor getSynchContext(RepeatContext context) {
		if (context == null || context.hasAttribute(SYNCHS_ATTR_KEY) || context.getParent() == null) {
			return context;
		}
		return getSynchContext(context.getParent());
	}

}
