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
package org.springframework.batch.core.step.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simplest possible implementation of {@link ItemHandler} with no skipping or
 * recovering. Just delegates all calls to the provided {@link ItemReader} and
 * {@link ItemWriter}.
 * 
 * Provides extension points by protected {@link #read(StepContribution)} and
 * {@link #write(Object, StepContribution)} methods that can be overriden to
 * provide more sophisticated behavior (e.g. skipping).
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class SimpleItemHandler implements ItemHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private ItemReader itemReader;

	private ItemWriter itemWriter;

	/**
	 * @param itemReader
	 * @param itemWriter
	 */
	public SimpleItemHandler(ItemReader itemReader, ItemWriter itemWriter) {
		super();
		this.itemReader = itemReader;
		this.itemWriter = itemWriter;
	}

	/**
	 * Get the next item from {@link #read(StepContribution)} and if not null
	 * pass the item to {@link #write(Object, StepContribution)}.
	 * 
	 * @see org.springframework.batch.core.step.item.ItemHandler#handle(org.springframework.batch.core.StepContribution)
	 */
	public ExitStatus handle(StepContribution contribution) throws Exception {
		Object item = read(contribution);
		if (item == null) {
			return ExitStatus.FINISHED;
		}
		contribution.incrementItemCount();
		write(item, contribution);
		return ExitStatus.CONTINUABLE;
	}

	/**
	 * @param contribution current context
	 * @return next item for writing
	 */
	protected Object read(StepContribution contribution) throws Exception {
		return doRead();
	}

	/**
	 * @return item
	 * @throws Exception
	 */
	protected final Object doRead() throws Exception {
		return itemReader.read();
	}

	/**
	 * 
	 * @param item the item to write
	 * @param contribution current context
	 */
	protected void write(Object item, StepContribution contribution) throws Exception {
		doWrite(item);
	}

	/**
	 * @param item
	 * @throws Exception
	 */
	protected final void doWrite(Object item) throws Exception {
		itemWriter.write(item);
	}

	/**
	 * @throws MarkFailedException
	 * @see org.springframework.batch.item.ItemReader#mark()
	 */
	public void mark() throws MarkFailedException {
		itemReader.mark();
	}

	/**
	 * @throws ResetFailedException
	 * @see org.springframework.batch.item.ItemReader#reset()
	 */
	public void reset() throws ResetFailedException {
		itemReader.reset();
	}

	/**
	 * @throws ClearFailedException
	 * @see org.springframework.batch.item.ItemWriter#clear()
	 */
	public void clear() throws ClearFailedException {
		itemWriter.clear();
	}

	/**
	 * @throws FlushFailedException
	 * @see org.springframework.batch.item.ItemWriter#flush()
	 */
	public void flush() throws FlushFailedException {
		itemWriter.flush();
	}

}
