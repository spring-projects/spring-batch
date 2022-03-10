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

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.metrics.BatchMetrics;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

/**
 * Simple implementation of the ChunkProvider interface that does basic chunk
 * providing from an {@link ItemReader}.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @see ChunkOrientedTasklet
 */
public class SimpleChunkProvider<I> implements ChunkProvider<I> {

	protected final Log logger = LogFactory.getLog(getClass());

	protected final ItemReader<? extends I> itemReader;

	private final MulticasterBatchListener<I, ?> listener = new MulticasterBatchListener<>();

	private final RepeatOperations repeatOperations;

	public SimpleChunkProvider(ItemReader<? extends I> itemReader, RepeatOperations repeatOperations) {
		this.itemReader = itemReader;
		this.repeatOperations = repeatOperations;
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 *
	 * @param listeners list of {@link StepListener}s.
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
	protected MulticasterBatchListener<I, ?> getListener() {
		return listener;
	}

	/**
	 * Surrounds the read call with listener callbacks.
	 * @return the item or {@code null} if the data source is exhausted
	 * @throws Exception is thrown if error occurs during read.
	 */
	@Nullable
	protected final I doRead() throws Exception {
		try {
			listener.beforeRead();
			I item = itemReader.read();
			if(item != null) {
				listener.afterRead(item);
			}
			return item;
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug(e.getMessage() + " : " + e.getClass().getName());
			}
			listener.onReadError(e);
			throw e;
		}
	}

	@Override
	public Chunk<I> provide(final StepContribution contribution) throws Exception {

		final Chunk<I> inputs = new Chunk<>();
		repeatOperations.iterate(new RepeatCallback() {

			@Override
			public RepeatStatus doInIteration(final RepeatContext context) throws Exception {
				I item = null;
				Timer.Sample sample = Timer.start(Metrics.globalRegistry);
				String status = BatchMetrics.STATUS_SUCCESS;
				try {
					item = read(contribution, inputs);
				}
				catch (SkipOverflowException e) {
					// read() tells us about an excess of skips by throwing an
					// exception
					status = BatchMetrics.STATUS_FAILURE;
					return RepeatStatus.FINISHED;
				}
				finally {
					stopTimer(sample, contribution.getStepExecution(), status);
				}
				if (item == null) {
					inputs.setEnd();
					return RepeatStatus.FINISHED;
				}
				inputs.add(item);
				contribution.incrementReadCount();
				return RepeatStatus.CONTINUABLE;
			}

		});

		return inputs;

	}

	private void stopTimer(Timer.Sample sample, StepExecution stepExecution, String status) {
		sample.stop(BatchMetrics.createTimer("item.read", "Item reading duration",
				Tag.of("job.name", stepExecution.getJobExecution().getJobInstance().getJobName()),
				Tag.of("step.name", stepExecution.getStepName()),
				Tag.of("status", status)
		));
	}

	@Override
	public void postProcess(StepContribution contribution, Chunk<I> chunk) {
		// do nothing
	}

	/**
	 * Delegates to {@link #doRead()}. Subclasses can add additional behaviour
	 * (e.g. exception handling).
	 *
	 * @param contribution the current step execution contribution
	 * @param chunk the current chunk
	 * @return a new item for processing or {@code null} if the data source is exhausted
	 *
	 * @throws SkipOverflowException if specifically the chunk is accumulating
	 * too much data (e.g. skips) and it wants to force a commit.
	 *
	 * @throws Exception if there is a generic issue
	 */
	@Nullable
	protected I read(StepContribution contribution, Chunk<I> chunk) throws SkipOverflowException, Exception {
		return doRead();
	}

}
