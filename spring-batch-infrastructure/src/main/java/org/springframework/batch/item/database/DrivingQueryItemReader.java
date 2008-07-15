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
package org.springframework.batch.item.database;

import java.util.Iterator;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>
 * Convenience class for driving query item readers. Item readers of this type
 * use a 'driving query' to return back a list of keys. A key can be defined as
 * anything that can uniquely identify a record so that a more detailed record
 * can be retrieved for each object. This allows a much smaller footprint to be
 * stored in memory for processing. The following 'Customer' example table will
 * help illustrate this:
 * 
 * <pre>
 * CREATE TABLE CUSTOMER (
 *   ID BIGINT IDENTITY PRIMARY KEY,  
 *   NAME VARCHAR(45),
 *   CREDIT FLOAT
 * );
 * </pre>
 * 
 * <p>
 * A cursor based solution would simply open up a cursor over ID, NAME, and
 * CREDIT, and move it from one to the next. This can cause issues on databases
 * with pessimistic locking strategies. A 'driving query' approach would be to
 * return only the ID of the customer, then use a separate DAO to retrieve the
 * name and credit for each ID. This means that there will be a call to a
 * separate DAO for each call to {@link ItemReader#read()}.
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
 * The implementation is *not* thread-safe.
 * 
 * 
 * @author Lucas Ward
 */
public class DrivingQueryItemReader implements ItemReader, InitializingBean, ItemStream {

	private boolean initialized = false;

	private List<Object> keys;

	private Iterator<Object> keysIterator;

	private int currentIndex = 0;

	private int lastCommitIndex = 0;

	private KeyCollector keyCollector;

	private boolean saveState = false;

	public DrivingQueryItemReader() {

	}

	/**
	 * Initialize the input source with the provided keys list.
	 * 
	 * @param keys
	 */
	public DrivingQueryItemReader(List<Object> keys) {
		this.keys = keys;
		this.keysIterator = keys.iterator();
	}

	/**
	 * Return the next key in the List.
	 * 
	 * @return next key in the list if not index is not at the last element,
	 * null otherwise.
	 */
	public Object read() {

		if (keysIterator.hasNext()) {
			currentIndex++;
			return keysIterator.next();
		}

		return null;
	}

	/**
	 * Get the current key. This method will return the same object returned by
	 * the last read() method. If no items have been read yet the ItemReader
	 * yet, then null will be returned.
	 * 
	 * @return the current key.
	 */
	protected Object getCurrentKey() {
		if (initialized && currentIndex > 0) {
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
	 * Initialize the item reader by delegating to the subclass in order to
	 * retrieve the keys.
	 * 
	 * @throws IllegalStateException if the keys list is null or initialized is
	 * true.
	 */
	public void open(ExecutionContext executionContext) {

		Assert.state(keys == null && !initialized, "Cannot open an already opened item reader"
				+ ", call close() first.");
		keys = keyCollector.retrieveKeys(executionContext);
		Assert.notNull(keys, "Keys must not be null");
		keysIterator = keys.listIterator();
		initialized = true;
	}

	public void update(ExecutionContext executionContext) {
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			if (getCurrentKey() != null) {
				keyCollector.updateContext(getCurrentKey(), executionContext);
			}
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(keyCollector, "The KeyGenerator must not be null.");
	}

	/**
	 * Set the key generation strategy to use for this input source.
	 * 
	 * @param keyCollector
	 */
	public void setKeyCollector(KeyCollector keyCollector) {
		this.keyCollector = keyCollector;
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
	 * @see
	 * org.springframework.batch.io.support.AbstractTransactionalIoSource#reset
	 * (org.springframework.batch.item.ExecutionContext)
	 */
	public void reset() {
		keysIterator = keys.listIterator(lastCommitIndex);
		currentIndex = lastCommitIndex;
	}

	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}
}
