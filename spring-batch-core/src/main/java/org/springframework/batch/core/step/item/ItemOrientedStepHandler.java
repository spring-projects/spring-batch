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
import org.springframework.core.AttributeAccessor;

/**
 * Simplest possible implementation of {@link StepHandler} with no skipping or
 * recovering. Just delegates all calls to the provided {@link ItemReader} and
 * {@link ItemWriter}.
 * 
 * Provides extension points by protected {@link #read(StepContribution)} and
 * {@link #write(List, StepContribution)} methods that can be overriden to
 * provide more sophisticated behavior (e.g. skipping).
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class ItemOrientedStepHandler<T, S> implements StepHandler {

	private static final String INPUT_BUFFER_KEY = "INPUT_BUFFER_KEY";

	private static final String OUTPUT_BUFFER_KEY = "OUTPUT_BUFFER_KEY";

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
	 * pass the item to {@link #write(List, StepContribution)}. If the
	 * {@link ItemProcessor} returns null, the write is omitted and another item
	 * taken from the reader.
	 * 
	 * @see org.springframework.batch.core.step.item.StepHandler#handle(org.springframework.batch.core.StepContribution,
	 * AttributeAccessor)
	 */
	public ExitStatus handle(final StepContribution contribution, AttributeAccessor attributes) throws Exception {

		final List<ItemWrapper<T>> inputs = getInputBuffer(attributes);
		final List<ItemWrapper<S>> outputs = getOutputBuffer(attributes);

		ExitStatus result = ExitStatus.CONTINUABLE;

		if (inputs.isEmpty() && outputs.isEmpty()) {

			result = repeatOperations.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(final RepeatContext context) throws Exception {
					ItemWrapper<T> item = read(contribution);
					contribution.incrementReadSkipCount(item.getSkipCount());
					if (item.getItem() == null) {
						return ExitStatus.FINISHED;
					}
					inputs.add(item);
					return ExitStatus.CONTINUABLE;
				}
			});

			storeInputs(attributes, inputs);

		}

		for (Iterator<ItemWrapper<T>> iterator = inputs.iterator(); iterator.hasNext();) {

			ItemWrapper<T> item = iterator.next();
			S output = null;

			// TODO: processor listener
			output = itemProcessor.process(item.getItem());

			// TODO: segregate read / write / filter count
			// (this is read count)
			contribution.incrementItemCount();

			// TODO: increment filter count if this is null
			if (output != null) {
				outputs.add(new ItemWrapper<S>(output));
			}

		}

		storeOutputsAndClearInputs(attributes, outputs, contribution);

		// TODO: use ItemWriter interface properly
		// TODO: make sure exceptions get handled by the appropriate handler
		write(outputs, contribution);

		// On successful completion clear the attributes to signal that there is
		// no more processing
		clearAll(attributes);

		logger.info("Contribution: " + contribution);
		return result;

	}

	/**
	 * @param attributes
	 */
	private void clearInputs(AttributeAccessor attributes) {
		attributes.removeAttribute(INPUT_BUFFER_KEY);
	}

	/**
	 * @param attributes
	 * @param inputs
	 */
	private void storeInputs(AttributeAccessor attributes, List<ItemWrapper<T>> inputs) {
		store(attributes, INPUT_BUFFER_KEY, inputs);
	}

	/**
	 * Savepoint at end of processing and before writing. The processed items
	 * ready for output are stored so that if writing fails they can be picked
	 * up again in the next try. The inputs are finished with so we can clear
	 * their attribute.
	 * 
	 * @param attributes
	 * @param outputs
	 */
	private void storeOutputsAndClearInputs(AttributeAccessor attributes, List<ItemWrapper<S>> outputs,
			StepContribution contribution) {
		store(attributes, OUTPUT_BUFFER_KEY, outputs);
		clearInputs(attributes);
	}

	/**
	 * @param attributes
	 * @param inputBufferKey
	 * @param outputs
	 */
	private <W> void store(AttributeAccessor attributes, String key, W value) {
		attributes.setAttribute(key, value);
	}

	private void clearAll(AttributeAccessor attributes) {
		for (String key : attributes.attributeNames()) {
			attributes.removeAttribute(key);
		}
	}

	private List<ItemWrapper<T>> getInputBuffer(AttributeAccessor attributes) {
		return getBuffer(attributes, INPUT_BUFFER_KEY);
	}

	private List<ItemWrapper<S>> getOutputBuffer(AttributeAccessor attributes) {
		return getBuffer(attributes, OUTPUT_BUFFER_KEY);
	}

	private <W> List<W> getBuffer(AttributeAccessor attributes, String key) {
		if (!attributes.hasAttribute(key)) {
			return new ArrayList<W>();
		}
		@SuppressWarnings("unchecked")
		List<W> resource = (List<W>) attributes.getAttribute(key);
		return resource;
	}

	/**
	 * @param contribution current context
	 * @return next item for writing
	 */
	protected ItemWrapper<T> read(StepContribution contribution) throws Exception {
		return new ItemWrapper<T>(doRead());
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
	 * @param items the item to write
	 * @param contribution current context
	 */
	protected void write(List<ItemWrapper<S>> items, StepContribution contribution) throws Exception {
		for (ItemWrapper<S> item : items) {
			doWrite(item);
		}
	}

	/**
	 * @param item
	 * @throws Exception
	 */
	protected final void doWrite(ItemWrapper<S> item) throws Exception {
		// TODO: increment write count
		itemWriter.write(Collections.singletonList(item.getItem()));
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	static protected class ItemWrapper<T> {

		final private T item;

		final private int skipCount;

		/**
		 * @param item
		 */
		public ItemWrapper(T item) {
			this(item, 0);
		}

		/**
		 * @param item
		 * @param skipCount
		 */
		public ItemWrapper(T item, int skipCount) {
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

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			return String.format("[%s,%d]", item, skipCount);
		}

	}

}
