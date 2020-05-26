/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.step.item;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.MulticasterBatchListener;
import org.springframework.batch.core.step.item.Chunk;
import org.springframework.batch.core.step.item.ChunkProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * {@link ChunkProcessor} implementation that implements JSR-352's chunking pattern
 * (read and process in a loop until the chunk is complete then write).  This
 * implementation is responsible for all three phases of chunk based processing
 * (reading, processing and writing).
 *
 * @author Michael Minella
 *
 * @param <I> The input type for the step
 * @param <O> The output type for the step
 */
public class JsrChunkProcessor<I,O> implements ChunkProcessor<I> {

	private final Log logger = LogFactory.getLog(getClass());
	private ItemReader<? extends I> itemReader;
	private final MulticasterBatchListener<I, O> listener = new MulticasterBatchListener<>();
	private RepeatOperations repeatTemplate;
	private ItemProcessor<? super I, ? extends O> itemProcessor;
	private ItemWriter<? super O> itemWriter;

	public JsrChunkProcessor() {
		this(null, null, null, null);
	}

	public JsrChunkProcessor(ItemReader<? extends I> reader, ItemProcessor<? super I, ? extends O> processor, ItemWriter<? super O> writer, RepeatOperations repeatTemplate) {
		this.itemReader = reader;
		this.itemProcessor = processor;
		this.itemWriter = writer;
		this.repeatTemplate = repeatTemplate;
	}

	protected MulticasterBatchListener<I, O> getListener() {
		return listener;
	}

	/**
	 * Loops through reading (via {@link #provide(StepContribution, Chunk)} and
	 * processing (via {@link #transform(StepContribution, Object)}) until the chunk
	 * is complete.  Once the chunk is complete, the results are written (via
	 * {@link #persist(StepContribution, Chunk)}.
	 *
	 * @see ChunkProcessor#process(StepContribution, Chunk)
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 */
	@Override
	public void process(final StepContribution contribution, final Chunk<I> chunk)
			throws Exception {

		final AtomicInteger filterCount = new AtomicInteger(0);
		final Chunk<O> output = new Chunk<>();

		repeatTemplate.iterate(new RepeatCallback() {

			@Override
			public RepeatStatus doInIteration(RepeatContext context) throws Exception {
				I item = provide(contribution, chunk);

				if(item != null) {
					contribution.incrementReadCount();
				} else {
					return RepeatStatus.FINISHED;
				}

				O processedItem = transform(contribution, item);

				if(processedItem == null) {
					filterCount.incrementAndGet();
				} else {
					output.add(processedItem);
				}

				return RepeatStatus.CONTINUABLE;
			}
		});

		contribution.incrementFilterCount(filterCount.get());
		if(output.size() > 0) {
			persist(contribution, output);
		}
	}

	/**
	 * Register some {@link StepListener}s with the handler. Each will get the
	 * callbacks in the order specified at the correct stage.
	 *
	 * @param listeners list of listeners to be used within this step
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
	 * Responsible for the reading portion of the chunking loop.  In this implementation, delegates
	 * to {@link #doProvide(StepContribution, Chunk)}
	 *
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 * @return an item
	 * @throws Exception thrown if error occurs during the reading portion of the chunking loop.
	 */
	protected I provide(final StepContribution contribution, final Chunk<I> chunk) throws Exception {
		return doProvide(contribution, chunk);
	}

	/**
	 * Implements reading as well as any related listener calls required.
	 *
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 * @return an item
	 * @throws Exception thrown if error occurs during reading or listener calls.
	 */
	protected final I doProvide(final StepContribution contribution, final Chunk<I> chunk) throws Exception {
		try {
			listener.beforeRead();
			I item = itemReader.read();
			if(item != null) {
				listener.afterRead(item);
			} else {
				chunk.setEnd();
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

	/**
	 * Responsible for the processing portion of the chunking loop.  In this implementation, delegates to the
	 * {@link #doTransform(Object)} if a processor is available (returns the item unmodified if it is not)
	 *
	 * @param contribution a {@link StepContribution}
	 * @param item an item
	 * @return a processed item if a processor is present (the unmodified item if it is not)
	 * @throws Exception thrown if error occurs during the processing portion of the chunking loop.
	 */
	protected O transform(final StepContribution contribution, final I item) throws Exception {
		if (itemProcessor == null) {
			@SuppressWarnings("unchecked")
			O result = (O) item;
			return result;
		}

		return doTransform(item);
	}

	/**
	 * Implements processing and all related listener calls.
	 *
	 * @param item the item to be processed
	 * @return the processed item
	 * @throws Exception thrown if error occurs during processing.
	 */
	protected final O doTransform(I item) throws Exception {
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
	 * Responsible for the writing portion of the chunking loop.  In this implementation, delegates to the
	 * {{@link #doPersist(StepContribution, Chunk)}.
	 *
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 * @throws Exception thrown if error occurs during the writing portion of the chunking loop.
	 */
	protected void persist(final StepContribution contribution, final Chunk<O> chunk) throws Exception {
		doPersist(contribution, chunk);

		contribution.incrementWriteCount(chunk.getItems().size());
	}

	/**
	 * Implements writing and all related listener calls
	 *
	 * @param contribution a {@link StepContribution}
	 * @param chunk a {@link Chunk}
	 * @throws Exception thrown if error occurs during the writing portion of the chunking loop.
	 */
	protected final void doPersist(final StepContribution contribution, final Chunk<O> chunk) throws Exception {
		try {
			List<O> items = chunk.getItems();
			listener.beforeWrite(items);
			itemWriter.write(items);
			listener.afterWrite(items);
		}
		catch (Exception e) {
			listener.onWriteError(e, chunk.getItems());
			throw e;
		}
	}
}
