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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

	private final ItemReader<? extends T> itemReader;

	private final ItemProcessor<? super T, ? extends S> itemProcessor;

	private final ItemWriter<? super S> itemWriter;

	private final RepeatOperations repeatOperations;

	/**
	 * @param itemReader
	 * @param itemProcessor
	 * @param itemWriter
	 * @param repeatOperations
	 */
	public ItemOrientedStepHandler(ItemReader<? extends T> itemReader,
			ItemProcessor<? super T, ? extends S> itemProcessor, ItemWriter<? super S> itemWriter,
			RepeatOperations repeatOperations) {
		super();
		this.itemReader = itemReader;
		this.itemProcessor = itemProcessor;
		this.itemWriter = itemWriter;
		this.repeatOperations = repeatOperations;
	}

	/**
	 * Get the next item from {@link #read(StepContribution)} and if not null
	 * pass the item to {@link #write(Object, StepContribution)}. If the
	 * {@link ItemProcessor} returns null, the write is omitted and another item
	 * taken from the reader.
	 * 
	 * @see org.springframework.batch.core.step.item.StepHandler#handle(org.springframework.batch.core.StepContribution)
	 */
	public ExitStatus handle(final StepContribution contribution) throws Exception {

		final List<ReadWrapper<T>> buffer = getItemBuffer();

		ExitStatus result = ExitStatus.CONTINUABLE;

		if (buffer.isEmpty()) {

			result = repeatOperations.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(final RepeatContext context) throws Exception {
					ReadWrapper<T> item = read(contribution);
					if (item == null) {
						return ExitStatus.FINISHED;
					}
					contribution.incrementReadSkipCount(item.getSkipCount());
					buffer.add(item);
					return ExitStatus.CONTINUABLE;
				}
			});

		}

		List<S> processed = new ArrayList<S>();

		for (Iterator<ReadWrapper<T>> iterator = buffer.iterator(); iterator.hasNext();) {

			ReadWrapper<T> item = iterator.next();
			S output = null;

			// TODO: segregate read / write / filter count
			// (this is read count)
			contribution.incrementItemCount();
			// TODO: processor listener
			output = itemProcessor.process(item.getItem());

			// TODO: increment filter count if this is null
			if (output != null) {
				processed.add(output);
			}

		}

		// TODO: use ItemWriter interface properly
		// TODO: make sure exceptions get handled by the appropriate handler
		for (S data : processed) {
			write(data, contribution);
		}
		buffer.clear();

		logger.info("Contribution: " + contribution);
		return result;

	}

	private List<ReadWrapper<T>> getItemBuffer() {
		if (!TransactionSynchronizationManager.hasResource(this)) {
			TransactionSynchronizationManager.bindResource(this, new ArrayList<ReadWrapper<T>>());
		}
		@SuppressWarnings("unchecked")
		List<ReadWrapper<T>> resource = (List<ReadWrapper<T>>) TransactionSynchronizationManager.getResource(this);
		return resource;
	}

	/**
	 * @param contribution current context
	 * @return next item for writing
	 */
	protected ReadWrapper<T> read(StepContribution contribution) throws Exception {
		T item = doRead();
		return item==null ? null : new ReadWrapper<T>(item);
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
	 */
	protected void write(S item, StepContribution contribution) throws Exception {
		doWrite(item);
	}

	/**
	 * @param item
	 * @throws Exception
	 */
	protected final void doWrite(S item) throws Exception {
		// TODO: increment write count
		itemWriter.write(Collections.singletonList(item));
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	static protected class ReadWrapper<T> {

		final private T item;

		final private int skipCount;

		/**
		 * @param item
		 */
		public ReadWrapper(T item) {
			this(item, 0);
		}

		/**
		 * @param item
		 * @param skipCount
		 */
		public ReadWrapper(T item, int skipCount) {
			this.item = item;
			this.skipCount = skipCount;
		}

		/**
		 * @return the item we are wrapping
		 */
		public T getItem() {
			return item;
		}

		/**
		 * Public getter for the skipCount.
		 * @return the skipCount
		 */
		public int getSkipCount() {
			return skipCount;
		}

	}

}
