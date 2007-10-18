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

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.util.Assert;

/**
 * <p>Abstract base class for driving query input sources.  Input Sources of
 * this type use a 'driving query' to return back a list of keys.  Upon each
 * call to read, a new key is returned.</p>
 *
 * <p>Mutability: Because this base class cannot guarantee that the keys returned
 * by subclasses are immutable, care should be taken to not modify a key value.
 * Doing so would cause issues if a rollback occurs.  For example, if a call
 * to read() is made, and the returned key is modified, a rollback will cause
 * the next call to read() to return the same object that was originally returned,
 * since there is no way to create a defensive copy, and re-querying the database
 * for all the keys would be too resource intensive.</p>
 *
 *
 * @author Lucas Ward
 *
 */
public abstract class AbstractDrivingQueryInputSource implements InputSource, ResourceLifecycle,
	DisposableBean, Restartable, InitializingBean {

	private boolean initialized = false;

	private List keys;

	private Iterator keysIterator;

	private int currentIndex = 0;

	private int lastCommitIndex = 0;

	private TransactionSynchronization synchronization =
		new DrivingQueryInputSourceTransactionSynchronization();

	/**
	 * Return the next key in the List.  If the InputSource has not been initialized yet,
	 * then {@link AbstractDrivingQueryInputSource.open()} will be called.
	 *
	 * @return next key in the list if not index is not at the last element, null otherwise.
	 */
	public Object read() {
		if (!initialized) {
			open();
		}

		if (keysIterator.hasNext()) {
			currentIndex++;
			return keysIterator.next();
		}

		return null;
	}

	/**
	 * Get the current key.  This method will return the same
	 * object returned by the last read() method.  If the
	 * InputSource hasn't been initialized yet, then null will
	 * be returned.
	 *
	 * @return the current key.
	 */
	protected Object getCurrentKey(){
		if(initialized){
			return keys.get(currentIndex - 1);
		}

		return null;
	}

	/**
	 * Close the resource by setting the list of keys to null, allowing them
	 * to be garbage collected.
	 */
	public void close() {
		initialized = false;
		currentIndex = 0;
		lastCommitIndex = 0;
		keys = null;
		keysIterator = null;
	}

	/**
	 * Initialize the input source by delegating to the subclass in order to retrieve
	 * the keys.  The input source will also be registered with the
	 * {@link BatchTransactionSynchronizationManager} in order to ensure it is notified
	 * about commits and rollbacks.
	 *
	 * @throws IllegalStateException if the keys list is null or initialized is true.
	 */
	public void open() {

		Assert.state(keys == null || initialized, "Cannot open an already opened input source" +
				", call close() first.");
		keys = retrieveKeys();
		keysIterator = keys.listIterator();
		BatchTransactionSynchronizationManager.registerSynchronization(synchronization);
		initialized = true;
	}

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.DisposableBean#destroy()
	 */
	public void destroy() throws Exception {
		close();
	}

	/**
	 * Restore input source to previous state. If the input source has already
	 * been initialized before calling restore (meaning, read has been called)
	 * then an IllegalStateException will be thrown, since all input sources
	 * should be restored before being read from, otherwise already processed
	 * data could be returned. The RestartData attempting to be restored from
	 * must have been obtained from the <strong>same input source as the one
	 * being restored from</strong> otherwise it is invalid.
	 *
	 * @throws IllegalArgumentException if restart data or it's properties is null.
	 * @throws IllegalStateException if the input source has already been intialized.
	 */
	public void restoreFrom(RestartData data) {

		Assert.notNull(data, "RestartData must not be null.");
		Assert.notNull(data.getProperties(), "RestartData properties must not be null.");
		Assert.state(!initialized, "Cannot restore when already intialized.  Call"
					+ " close() first before restore()");

		if (data.getProperties().size() == 0) {
			return;
		}

		keys = restoreKeys(data);

		if(keys != null && keys.size() > 0){
			keysIterator = keys.listIterator();
			initialized = true;
		}
	}

	//Abstract Methods
	/**
	 * @return list of keys returned by the driving query
	 */
	protected abstract List retrieveKeys();

	/**
	 * Restore the keys list based on provided restart data.
	 *
	 * @param restartData, the restart data to restore the keys list from.
	 * @return a list of keys.
	 */
	protected abstract List restoreKeys(RestartData restartData);

	/**
	 * Encapsulates transaction events handling.
	 */
	private class DrivingQueryInputSourceTransactionSynchronization extends TransactionSynchronizationAdapter {
		public void afterCompletion(int status) {
			if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				keysIterator = keys.listIterator(lastCommitIndex);
			} else if (status == TransactionSynchronization.STATUS_COMMITTED) {
				lastCommitIndex = currentIndex;
			}
		}
	}
}
