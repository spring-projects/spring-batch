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

import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.MarkFailedException;
import org.springframework.batch.item.ResetFailedException;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Simplest possible implementation of {@link StepHandler} with no skipping or
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
public class ItemOrientedStepHandler<T, S> implements StepHandler {

	protected final Log logger = LogFactory.getLog(getClass());

	private ItemReader<? extends T> itemReader;

	private ItemProcessor<? super T, ? extends S> itemProcessor;

	private ItemWriter<? super S> itemWriter;

	/**
	 * @param itemReader
	 * @param itemProcessor
	 * @param itemWriter
	 */
	public ItemOrientedStepHandler(ItemReader<? extends T> itemReader,
			ItemProcessor<? super T, ? extends S> itemProcessor, ItemWriter<? super S> itemWriter) {
		super();
		this.itemReader = itemReader;
		this.itemProcessor = itemProcessor;
		this.itemWriter = itemWriter;
	}

	/**
	 * Get the next item from {@link #read(StepContribution)} and if not null
	 * pass the item to {@link #write(Object, StepContribution)}. If the
	 * {@link ItemProcessor} returns null, the write is omitted and another
	 * item taken from the reader.
	 * 
	 * @see org.springframework.batch.core.step.item.StepHandler#handle(org.springframework.batch.core.StepContribution)
	 */
	public ExitStatus handle(StepContribution contribution) throws Exception {
		boolean processed = false;
		while (!processed) {
			T item = read(contribution);
			if (item == null) {
				return ExitStatus.FINISHED;
			}
			// TODO: segregate read / write / filter count
			contribution.incrementItemCount();
			processed = write(item, contribution);
		}
		return ExitStatus.CONTINUABLE;
	}

	/**
	 * @param contribution current context
	 * @return next item for writing
	 */
	protected T read(StepContribution contribution) throws Exception {
		return doRead();
	}

	/**
	 * @return item
	 * @throws Exception
	 */
	protected final T doRead() throws Exception {
		return itemReader.read();
	}

	/**
	 * 
	 * @param item the item to write
	 * @param contribution current context
	 * @return true if the item was written (as opposed to filtered)
	 */
	protected boolean write(T item, StepContribution contribution) throws Exception {
		return doWrite(item);
	}

	/**
	 * @param item
	 * @throws Exception
	 */
	protected final boolean doWrite(T item) throws Exception {
		S processed = itemProcessor.process(item);
		if (processed != null) {
			// TODO: increment filtered item count
			itemWriter.write(Collections.singletonList(processed));
			return true;
		}
		return false;
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

}
