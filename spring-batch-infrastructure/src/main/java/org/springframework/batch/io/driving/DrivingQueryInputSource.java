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
package org.springframework.batch.io.driving;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.io.InputSource;
import org.springframework.batch.io.support.AbstractTransactionalIoSource;
import org.springframework.batch.item.ResourceLifecycle;
import org.springframework.batch.repeat.synch.BatchTransactionSynchronizationManager;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>
 * Convenience class for driving query input sources. Input Sources of this type
 * use a 'driving query' to return back a list of keys. Upon each call to read,
 * a new key is returned. To use the input source, inject a key generation
 * strategy.
 * </p>
 * 
 * <p>
 * Mutability: Because this base class cannot guarantee that the keys returned
 * by subclasses are immutable, care should be taken to not modify a key value.
 * Doing so would cause issues if a rollback occurs. For example, if a call to
 * read() is made, and the returned key is modified, a rollback will cause the
 * next call to read() to return the same object that was originally returned,
 * since there is no way to create a defensive copy, and re-querying the
 * database for all the keys would be too resource intensive.
 * </p>
 * 
 * 
 * @author Lucas Ward
 * @since 1.0
 */
public class DrivingQueryInputSource extends AbstractTransactionalIoSource
		implements InputSource, ResourceLifecycle, InitializingBean,
		DisposableBean, Restartable {

	private boolean initialized = false;

	private List keys;

	private Iterator keysIterator;

	private int currentIndex = 0;

	private int lastCommitIndex = 0;

	private KeyGenerator keyGenerator;

	public DrivingQueryInputSource() {

	}

	/**
	 * Initialize the input source with the provided keys list.
	 * 
	 * @param keys
	 */
	public DrivingQueryInputSource(List keys) {
		this.keys = keys;
		this.keysIterator = keys.iterator();
	}

	/**
	 * Return the next key in the List. If the InputSource has not been
	 * initialized yet, then {@link AbstractDrivingQueryInputSource.open()} will
	 * be called.
	 * 
	 * @return next key in the list if not index is not at the last element,
	 *         null otherwise.
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
	 * Get the current key. This method will return the same object returned by
	 * the last read() method. If the InputSource hasn't been initialized yet,
	 * then null will be returned.
	 * 
	 * @return the current key.
	 */
	protected Object getCurrentKey() {
		if (initialized) {
			return keys.get(currentIndex - 1);
		}

		return null;
	}

	/**
	 * Close the resource by setting the list of keys to null, allowing them to
	 * be garbage collected.
	 */
	public void close() {
		initialized = false;
		currentIndex = 0;
		lastCommitIndex = 0;
		keys = null;
		keysIterator = null;
	}

	/**
	 * Initialize the input source by delegating to the subclass in order to
	 * retrieve the keys. The input source will also be registered with the
	 * {@link BatchTransactionSynchronizationManager} in order to ensure it is
	 * notified about commits and rollbacks.
	 * 
	 * @throws IllegalStateException
	 *             if the keys list is null or initialized is true.
	 */
	public void open() {

		Assert.state(keys == null || initialized,
				"Cannot open an already opened input source"
						+ ", call close() first.");
		keys = keyGenerator.retrieveKeys();
		keysIterator = keys.listIterator();
		super.registerSynchronization();
		initialized = true;
	}

	/*
	 * (non-Javadoc)
	 * 
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
	 * @throws IllegalArgumentException
	 *             if restart data or it's properties is null.
	 * @throws IllegalStateException
	 *             if the input source has already been initialized.
	 */
	public final void restoreFrom(RestartData data) {

		Assert.notNull(data, "RestartData must not be null.");
		Assert.notNull(data.getProperties(),
				"RestartData properties must not be null.");
		Assert.state(!initialized,
				"Cannot restore when already intialized.  Call"
						+ " close() first before restore()");

		if (data.getProperties().size() == 0) {
			return;
		}

		keys = keyGenerator.restoreKeys(data);

		if (keys != null && keys.size() > 0) {
			keysIterator = keys.listIterator();
			initialized = true;
		}
	}

	public RestartData getRestartData() {
		return keyGenerator.getKeyAsRestartData(getCurrentKey());
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(keyGenerator, "The KeyGenerator must not be null.");
	}

	/**
	 * Set the key generation strategy to use for this input source.
	 * 
	 * @param keyGenerator
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	protected void transactionCommitted() {
		lastCommitIndex = currentIndex;
	}

	protected void transactionRolledBack() {
		keysIterator = keys.listIterator(lastCommitIndex);
	}

}
