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

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link ChunkProcessor} interface that handles basic
 * item writing and processing.  Any exceptions encountered will be rethrown.
 * 
 * @see ChunkOrientedTasklet
 */
public class SimpleChunkProcessor<I, O> implements ChunkProcessor<I>, InitializingBean {

	private ItemProcessor<? super I, ? extends O> itemProcessor;

	private ItemWriter<? super O> itemWriter;

	private final MulticasterBatchListener<I, O> listener = new MulticasterBatchListener<I, O>();

	/**
	 * Default constructor for ease of configuration (both itemWriter and
	 * itemProcessor are mandatory).
	 */
	@SuppressWarnings("unused")
	private SimpleChunkProcessor() {
		this(null, null);
	}

	public SimpleChunkProcessor(ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter) {
		this.itemProcessor = itemProcessor;
		this.itemWriter = itemWriter;
	}

	/**
	 * @param itemProcessor the {@link ItemProcessor} to set
	 */
	public void setItemProcessor(ItemProcessor<? super I, ? extends O> itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/**
	 * @param itemWriter the {@link ItemWriter} to set
	 */
	public void setItemWriter(ItemWriter<? super O> itemWriter) {
		this.itemWriter = itemWriter;
	}

	/**
	 * Check mandatory properties.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemWriter, "ItemWriter must be set");
		Assert.notNull(itemProcessor, "ItemProcessor must be set");
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 * 
	 * @param listeners
	 */
	public void setListeners(List<? extends StepListener> listeners) {
		for (StepListener listener : listeners) {
			registerListener(listener);
		}
	}

	/**
	 * Register a listener for callbacks at the appropriate stages in a process.
	 * 
	 * @param listener a {@link StepListener}
	 */
	public void registerListener(StepListener listener) {
		this.listener.register(listener);
	}

	/**
	 * @return the listener
	 */
	protected MulticasterBatchListener<I, O> getListener() {
		return listener;
	}

	/**
	 * @param item the input item
	 * @return the result of the processing
	 * @throws Exception
	 */
	protected final O doProcess(I item) throws Exception {
		try {
			listener.beforeProcess(item);
			O result = itemProcessor.process(item);
			listener.afterProcess(item, result);
			return result;
		}
		catch (Exception e) {
			listener.onProcessError(item, e);
			throw e;
		}
	}

	/**
	 * Surrounds the actual write call with listener callbacks.
	 * 
	 * @param items
	 * @throws Exception
	 */
	protected final void doWrite(List<O> items) throws Exception {
		try {
			listener.beforeWrite(items);
			writeItems(items);
			doAfterWrite(items);
		}
		catch (Exception e) {
			listener.onWriteError(e, items);
			throw e;
		}
	}

	/**
	 * Call the listener's after write method.
	 * 
	 * @param items
	 */
	protected final void doAfterWrite(List<O> items) {
		listener.afterWrite(items);
	}

	protected void writeItems(List<O> items) throws Exception {
		itemWriter.write(items);
	}

	public final void process(StepContribution contribution, Chunk<I> inputs) throws Exception {

		/*
		 * Need to remember the write skips across transactions, otherwise they
		 * keep coming back. Since we register skips with the inputs they will
		 * not be processed again but the output skips need to be saved for
		 * registration later with the listeners. The inputs are going to be the
		 * same for all transactions processing the same chunk, but the outputs
		 * are not, so we stash them in user data on the inputs.
		 */

		@SuppressWarnings("unchecked")
		Chunk<O> skips = (Chunk<O>) inputs.getUserData();
		if (skips == null) {
			skips = new Chunk<O>();
		}

		// If there is no input we don't have to do anything more
		if (inputs.isEmpty() && skips.getSkips().isEmpty()) {
			return;
		}

		int inputsSize = inputs.size();

		Chunk<O> outputs = transform(contribution, inputs);

		contribution.incrementFilterCount(inputsSize  - outputs.size() - inputs.getSkips().size());

		boolean busy = skips.isBusy();
		outputs = new Chunk<O>(outputs.getItems(), skips.getSkips());
		outputs.setBusy(busy);

		// Remember for next time if there are skips accumulating
		inputs.setUserData(outputs);

		write(contribution, inputs, outputs);

	}

	/**
	 * Simple implementation delegates to the {@link #doWrite(List)} method and
	 * increments the write count in the contribution. Subclasses can handle
	 * more complicated scenarios, e.g.with fault tolerance. If output items are
	 * skipped they should be removed from the inputs as well.
	 * 
	 * @param contribution the current step contribution
	 * @param inputs the inputs that gave rise to the ouputs
	 * @param outputs the outputs to write
	 * @throws Exception if there is a problem
	 */
	protected void write(StepContribution contribution, Chunk<I> inputs, Chunk<O> outputs) throws Exception {
		doWrite(outputs.getItems());
		contribution.incrementWriteCount(outputs.size());
	}

	protected Chunk<O> transform(StepContribution contribution, Chunk<I> inputs) throws Exception {
		Chunk<O> outputs = new Chunk<O>();
		for (Chunk<I>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {
			final I item = iterator.next();
			O output = doProcess(item);
			if (output != null) {
				outputs.add(output);
			}
			else {
				iterator.remove();
			}
		}
		return outputs;
	}

}
