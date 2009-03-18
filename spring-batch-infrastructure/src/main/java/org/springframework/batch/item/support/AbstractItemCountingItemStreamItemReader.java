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

package org.springframework.batch.item.support;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
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
public abstract class AbstractItemCountingItemStreamItemReader<T> implements ItemReader<T>, ItemStream {

	private static final String READ_COUNT = "read.count";

	private static final String READ_COUNT_MAX = "read.count.max";

	private int currentItemCount = 0;

	private int maxItemCount = Integer.MAX_VALUE;

	private ExecutionContextUserSupport ecSupport = new ExecutionContextUserSupport();

	private boolean saveState = true;

	/**
	 * Read next item from input.
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

	public final T read() throws Exception, UnexpectedInputException, ParseException {
		if (currentItemCount >= maxItemCount-1) {
			return null;
		}
		currentItemCount++;
		return doRead();
	}

	protected int getCurrentItemCount() {
		return currentItemCount;
	}

	protected void setCurrentItemCount(int count) {
		this.currentItemCount = count;
	}

	public void close() throws ItemStreamException {
		currentItemCount = 0;
		try {
			doClose();
		}
		catch (Exception e) {
			throw new ItemStreamException("Error while closing item reader", e);
		}
	}

	public void open(ExecutionContext executionContext) throws ItemStreamException {

		try {
			doOpen();
		}
		catch (Exception e) {
			throw new ItemStreamException("Failed to initialize the reader", e);
		}

		if (executionContext.containsKey(ecSupport.getKey(READ_COUNT_MAX))) {
			maxItemCount = executionContext.getInt(ecSupport.getKey(READ_COUNT_MAX));
		}

		if (executionContext.containsKey(ecSupport.getKey(READ_COUNT))) {
			int itemCount = executionContext.getInt(ecSupport.getKey(READ_COUNT));

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

	public void update(ExecutionContext executionContext) throws ItemStreamException {
		if (saveState) {
			Assert.notNull(executionContext, "ExecutionContext must not be null");
			executionContext.putInt(ecSupport.getKey(READ_COUNT), currentItemCount);
			if (maxItemCount < Integer.MAX_VALUE) {
				executionContext.putInt(ecSupport.getKey(READ_COUNT_MAX), maxItemCount);
			}
		}

	}

	public void setName(String name) {
		ecSupport.setName(name);
	}

	/**
	 * Set the flag that determines whether to save internal data for
	 * {@link ExecutionContext}. Only switch this to false if you don't want to
	 * save any state from this stream, and you don't need it to be restartable.
	 * 
	 * @param saveState flag value (default true).
	 */
	public void setSaveState(boolean saveState) {
		this.saveState = saveState;
	}

}
