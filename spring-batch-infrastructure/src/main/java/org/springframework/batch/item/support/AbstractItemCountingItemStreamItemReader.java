/*
 * Copyright 2006-2013 the original author or authors.
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

package org.springframework.batch.item.support;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemCountAware;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.util.Assert;

/**
 * Abstract superclass for {@link ItemReader}s that supports restart by storing
 * item count in the {@link ExecutionContext} (therefore requires item ordering
 * to be preserved between runs).
 * 
 * Subclasses are inherently *not* thread-safe.
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractItemCountingItemStreamItemReader<T> extends AbstractItemStreamItemReader<T> {

	private static final String READ_COUNT = "read.count";

	private static final String READ_COUNT_MAX = "read.count.max";

	private int currentItemCount = 0;

	private int maxItemCount = Integer.MAX_VALUE;

	private boolean saveState = true;

	/**
	 * Read next item from input.
	 * 
	 * @return item
	 * @throws Exception
	 */
	protected abstract T doRead() throws Exception;

	/**
	 * Open resources necessary to start reading input.
	 */
	protected abstract void doOpen() throws Exception;

	/**
	 * Close the resources opened in {@link #doOpen()}.
	 */
	protected abstract void doClose() throws Exception;

	/**
	 * Move to the given item index. Subclasses should override this method if
	 * there is a more efficient way of moving to given index than re-reading
	 * the input using {@link #doRead()}.
	 */
	protected void jumpToItem(int itemIndex) throws Exception {
		for (int i = 0; i < itemIndex; i++) {
			read();
		}
	}

	@Override
	public T read() throws Exception, UnexpectedInputException, ParseException {
		if (currentItemCount >= maxItemCount) {
			return null;
		}
		currentItemCount++;
		T item = doRead();
		if(item instanceof ItemCountAware) {
			((ItemCountAware) item).setItemCount(currentItemCount);
		}
		return item;
	}

	protected int getCurrentItemCount() {
		return currentItemCount;
	}

	/**
	 * The index of the item to start reading from. If the
	 * {@link ExecutionContext} contains a key <code>[name].read.count</code>
	 * (where <code>[name]</code> is the name of this component) the value from
	 * the {@link ExecutionContext} will be used in preference.
	 * 
	 * @see #setName(String)
	 * 
	 * @param count the value of the current item count
	 */
	public void setCurrentItemCount(int count) {
		this.currentItemCount = count;
	}

	/**
	 * The maximum index of the items to be read. If the
	 * {@link ExecutionContext} contains a key
	 * <code>[name].read.count.max</code> (where <code>[name]</code> is the name
	 * of this component) the value from the {@link ExecutionContext} will be
	 * used in preference.
	 * 
	 * @see #setName(String)
	 * 
	 * @param count the value of the maximum item count
	 */
	public void setMaxItemCount(int count) {
		this.maxItemCount = count;
	}

	@Override
	public void close() throws ItemStreamException {
		super.close();
		currentItemCount = 0;
		try {
			doClose();
		}
		catch (Exception e) {
			throw new ItemStreamException("Error while closing item reader", e);
		}
	}

	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		super.open(executionContext);
		try {
			doOpen();
		}
		catch (Exception e) {
			throw new ItemStreamException("Failed to initialize the reader", e);
		}
		if (!isSaveState()) {
			return;
		}

		if (executionContext.containsKey(getExecutionContextKey(READ_COUNT_MAX))) {
			maxItemCount = executionContext.getInt(getExecutionContextKey(READ_COUNT_MAX));
		}

		if (executionContext.containsKey(getExecutionContextKey(READ_COUNT))) {
			int itemCount = executionContext.getInt(getExecutionContextKey(READ_COUNT));

			if (itemCount < maxItemCount) {
				try {
					jumpToItem(itemCount);
				}
				catch (Exception e) {
					throw new ItemStreamException("Could not move to stored position on restart", e);
				}
			}
			currentItemCount = itemCount;

		}

	}

	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		super.update(executionContext);
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putInt(getExecutionContextKey(READ_COUNT), currentItemCount);
			if (maxItemCount < Integer.MAX_VALUE) {
				executionContext.putInt(getExecutionContextKey(READ_COUNT_MAX), maxItemCount);
			}
		}

	}

	/**
	 * The name of the component which will be used as a stem for keys in the
	 * {@link ExecutionContext}. Subclasses should provide a default value, e.g.
	 * the short form of the class name.
	 * 
	 * @param name the name for the component
	 */
	public void setName(String name) {
		this.setExecutionContextName(name);
	}

	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to
	 * save any state from this stream, and you don't need it to be restartable.
	 * Always set it to false if the reader is being used in a concurrent
	 * environment.
	 * 
	 * @param saveState flag value (default true).
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

	/**
	 * The flag that determines whether to save internal state for restarts.
	 * @return true if the flag was set
	 */
	public boolean isSaveState() {
		return saveState;
	}

}
