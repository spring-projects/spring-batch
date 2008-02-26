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

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.KeyedItemReader;
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
public class DrivingQueryItemReader implements KeyedItemReader, InitializingBean,
		ItemStream {

	private boolean initialized = false;

	private List keys;

	private Iterator keysIterator;

	private int currentIndex = 0;

	private int lastCommitIndex = 0;

	private KeyGenerator keyGenerator;
	
	private boolean saveState = false;

	public DrivingQueryItemReader() {

	}

	/**
	 * Initialize the input source with the provided keys list.
	 * 
	 * @param keys
	 */
	public DrivingQueryItemReader(List keys) {
		this.keys = keys;
		this.keysIterator = keys.iterator();
	}

	/**
	 * Return the next key in the List. If the ItemReader has not been
	 * initialized yet, then {@link AbstractDrivingQueryItemReader.open()} will
	 * be called.
	 * 
	 * @return next key in the list if not index is not at the last element,
	 * null otherwise.
	 */
	public Object read() {
		if (!initialized) {
			open(new ExecutionContext());
		}

		if (keysIterator.hasNext()) {
			currentIndex++;
			return keysIterator.next();
		}

		return null;
	}

	/**
	 * Get the current key. This method will return the same object returned by
	 * the last read() method. If the ItemReader hasn't been initialized yet,
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
	public void close(ExecutionContext executionContext) {
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
	 * @throws IllegalStateException if the keys list is null or initialized is
	 * true.
	 */
	public void open(ExecutionContext executionContext) {

		Assert.state(keys == null && !initialized, "Cannot open an already opened input source"
				+ ", call close() first.");
		keys = keyGenerator.retrieveKeys(executionContext);
		keysIterator = keys.listIterator();
		initialized = true;
	}

	public void update(ExecutionContext executionContext) {
		if(saveState){
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			if(getCurrentKey() != null){
				keyGenerator.saveState(getCurrentKey(), executionContext);
			}
		}
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
		mark();
	}

	protected void transactionRolledBack() {
		reset();
	}

	/**
	 * Return the item itself (which is already a key).
	 * 
	 * @see org.springframework.batch.item.ItemReader#getKey(java.lang.Object)
	 */
	public Object getKey(Object item) {
		return item;
	}

	/**
	 * Mark is supported as long as this {@link ItemStream} is used in a
	 * single-threaded environment. The state backing the mark is a single
	 * counter, keeping track of the current position, so multiple threads
	 * cannot be accommodated.
	 * 
	 * @see org.springframework.batch.item.ItemReader#mark()
	 */
	public void mark() {
		lastCommitIndex = currentIndex;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.io.support.AbstractTransactionalIoSource#reset(org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		keysIterator = keys.listIterator(lastCommitIndex);
	}

	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
}
