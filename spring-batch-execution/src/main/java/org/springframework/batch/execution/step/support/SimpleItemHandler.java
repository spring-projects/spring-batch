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
package org.springframework.batch.execution.step.support;

import org.springframework.batch.core.domain.StepContribution;
import org.springframework.batch.execution.step.ItemHandler;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.exception.ClearFailedException;
import org.springframework.batch.item.exception.FlushFailedException;
import org.springframework.batch.item.exception.MarkFailedException;
import org.springframework.batch.item.exception.ResetFailedException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simplest possible implementation of {@link ItemHandler} with no skipping or
 * recovering. Just delegates all calls to the provided {@link ItemReader} and
 * {@link ItemWriter}.
 * 
 * @author Dave Syer
 * 
 */
public class SimpleItemHandler implements ItemHandler {

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
	 * Public getter for the ItemReader.
	 * @return the itemReader
	 */
	public ItemReader getItemReader() {
		return itemReader;
	}

	/**
	 * Public getter for the ItemWriter.
	 * @return the itemWriter
	 */
	public ItemWriter getItemWriter() {
		return itemWriter;
	}

	/**
	 * Read from the {@link ItemReader} and process (if not null) with the
	 * {@link ItemWriter}.
	 * 
	 * @see org.springframework.batch.execution.step.ItemHandler#handle(org.springframework.batch.core.domain.StepContribution)
	 */
	public ExitStatus handle(StepContribution contribution) throws Exception {
		Object item = itemReader.read();
		if (item == null) {
			return ExitStatus.FINISHED;
		}
		itemWriter.write(item);
		return ExitStatus.CONTINUABLE;
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
