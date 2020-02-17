/*
 * Copyright 2006-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.step.item;

import java.util.List;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.metrics.BatchMetrics;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Simple implementation of the {@link ChunkProcessor} interface that handles
 * basic item writing and processing. Any exceptions encountered will be
 * rethrown.
 *
 * @see ChunkOrientedTasklet
 */
public class SimpleChunkProcessor<I, O> implements ChunkProcessor<I>, InitializingBean {

	private ItemProcessor<? super I, ? extends O> itemProcessor;

	private ItemWriter<? super O> itemWriter;

	private final MulticasterBatchListener<I, O> listener = new MulticasterBatchListener<>();

	/**
	 * Default constructor for ease of configuration.
	 */
	@SuppressWarnings("unused")
	private SimpleChunkProcessor() {
		this(null, null);
	}

	public SimpleChunkProcessor(@Nullable ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter) {
		this.itemProcessor = itemProcessor;
		this.itemWriter = itemWriter;
	}

	public SimpleChunkProcessor(ItemWriter<? super O> itemWriter) {
		this(null, itemWriter);
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
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(itemWriter, "ItemWriter must be set");
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 *
	 * @param listeners list of {@link StepListener} instances.
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
	 * @throws Exception thrown if error occurs.
	 */
	protected final O doProcess(I item) throws Exception {

		if (itemProcessor == null) {
			@SuppressWarnings("unchecked")
			O result = (O) item;
			return result;
		}

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
	 * @param items list of items to be written.
	 * @throws Exception thrown if error occurs.
	 */
	protected final void doWrite(List<O> items) throws Exception {

		if (itemWriter == null) {
			return;
		}

		try {
			listener.beforeWrite(items);
			writeItems(items);
			doAfterWrite(items);
		}
		catch (Exception e) {
			doOnWriteError(e, items);
			throw e;
		}

	}

	/**
	 * Call the listener's after write method.
	 *
	 * @param items list of items that were just written.
	 */
	protected final void doAfterWrite(List<O> items) {
		listener.afterWrite(items);
	}

	/**
	 * Call listener's writerError method.
	 * @param e exception that occurred.
	 * @param items list of items that failed to be written.
	 */
	protected final void doOnWriteError(Exception e, List<O> items) {
		listener.onWriteError(e, items);
	}

	/**
	 * @param items list of items to be written.
	 * @throws Exception thrown if error occurs.
	 */
	protected void writeItems(List<O> items) throws Exception {
		if (itemWriter != null) {
			itemWriter.write(items);
		}
	}

	@Override
	public final void process(StepContribution contribution, Chunk<I> inputs) throws Exception {

		// Allow temporary state to be stored in the user data field
		initializeUserData(inputs);

		// If there is no input we don't have to do anything more
		if (isComplete(inputs)) {
			return;
		}

		// Make the transformation, calling remove() on the inputs iterator if
		// any items are filtered. Might throw exception and cause rollback.
		Chunk<O> outputs = transform(contribution, inputs);

		// Adjust the filter count based on available data
		contribution.incrementFilterCount(getFilterCount(inputs, outputs));

		// Adjust the outputs if necessary for housekeeping purposes, and then
		// write them out...
		write(contribution, inputs, getAdjustedOutputs(inputs, outputs));

	}

	/**
	 * Extension point for subclasses to allow them to memorise the contents of
	 * the inputs, in case they are needed for accounting purposes later. The
	 * default implementation sets up some user data to remember the original
	 * size of the inputs. If this method is overridden then some or all of
	 * {@link #isComplete(Chunk)}, {@link #getFilterCount(Chunk, Chunk)} and
	 * {@link #getAdjustedOutputs(Chunk, Chunk)} might also need to be, to
	 * ensure that the user data is handled consistently.
	 *
	 * @param inputs the inputs for the process
	 */
	protected void initializeUserData(Chunk<I> inputs) {
		inputs.setUserData(inputs.size());
	}

	/**
	 * Extension point for subclasses to calculate the filter count. Defaults to
	 * the difference between input size and output size.
	 *
	 * @param inputs the inputs after transformation
	 * @param outputs the outputs after transformation
	 *
	 * @return the difference in sizes
	 *
	 * @see #initializeUserData(Chunk)
	 */
	protected int getFilterCount(Chunk<I> inputs, Chunk<O> outputs) {
		return (Integer) inputs.getUserData() - outputs.size();
	}

	/**
	 * Extension point for subclasses that want to store additional data in the
	 * inputs. Default just checks if inputs are empty.
	 *
	 * @param inputs the input chunk
	 * @return true if it is empty
	 *
	 * @see #initializeUserData(Chunk)
	 */
	protected boolean isComplete(Chunk<I> inputs) {
		return inputs.isEmpty();
	}

	/**
	 * Extension point for subclasses that want to adjust the outputs based on
	 * additional saved data in the inputs. Default implementation just returns
	 * the outputs unchanged.
	 *
	 * @param inputs the inputs for the transformation
	 * @param outputs the result of the transformation
	 * @return the outputs unchanged
	 *
	 * @see #initializeUserData(Chunk)
	 */
	protected Chunk<O> getAdjustedOutputs(Chunk<I> inputs, Chunk<O> outputs) {
		return outputs;
	}

	/**
	 * Simple implementation delegates to the {@link #doWrite(List)} method and
	 * increments the write count in the contribution. Subclasses can handle
	 * more complicated scenarios, e.g.with fault tolerance. If output items are
	 * skipped they should be removed from the inputs as well.
	 *
	 * @param contribution the current step contribution
	 * @param inputs the inputs that gave rise to the outputs
	 * @param outputs the outputs to write
	 * @throws Exception if there is a problem
	 */
	protected void write(StepContribution contribution, Chunk<I> inputs, Chunk<O> outputs) throws Exception {
		Timer.Sample sample = BatchMetrics.createTimerSample();
		String status = BatchMetrics.STATUS_SUCCESS;
		try {
			doWrite(outputs.getItems());
		}
		catch (Exception e) {
			/*
			 * For a simple chunk processor (no fault tolerance) we are done
			 * here, so prevent any more processing of these inputs.
			 */
			inputs.clear();
			status = BatchMetrics.STATUS_FAILURE;
			throw e;
		}
		finally {
			stopTimer(sample, contribution.getStepExecution(), "chunk.write", status, "Chunk writing");
		}
		contribution.incrementWriteCount(outputs.size());
	}

	protected Chunk<O> transform(StepContribution contribution, Chunk<I> inputs) throws Exception {
		Chunk<O> outputs = new Chunk<>();
		for (Chunk<I>.ChunkIterator iterator = inputs.iterator(); iterator.hasNext();) {
			final I item = iterator.next();
			O output;
			Timer.Sample sample = BatchMetrics.createTimerSample();
			String status = BatchMetrics.STATUS_SUCCESS;
			try {
				output = doProcess(item);
			}
			catch (Exception e) {
				/*
				 * For a simple chunk processor (no fault tolerance) we are done
				 * here, so prevent any more processing of these inputs.
				 */
				inputs.clear();
				status = BatchMetrics.STATUS_FAILURE;
				throw e;
			}
			finally {
				stopTimer(sample, contribution.getStepExecution(), "item.process", status, "Item processing");
			}
			if (output != null) {
				outputs.add(output);
			}
			else {
				iterator.remove();
			}
		}
		return outputs;
	}

	protected void stopTimer(Timer.Sample sample, StepExecution stepExecution, String metricName, String status, String description) {
		sample.stop(BatchMetrics.createTimer(metricName, description + " duration",
				Tag.of("job.name", stepExecution.getJobExecution().getJobInstance().getJobName()),
				Tag.of("step.name", stepExecution.getStepName()),
				Tag.of("status", status)
		));
	}

}
