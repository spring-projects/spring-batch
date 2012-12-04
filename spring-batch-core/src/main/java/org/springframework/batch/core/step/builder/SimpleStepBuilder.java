/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.SimpleChunkProcessor;
import org.springframework.batch.core.step.item.SimpleChunkProvider;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepBuilder<I, O> extends AbstractTaskletStepBuilder<SimpleStepBuilder<I, O>> {

	private static final int DEFAULT_COMMIT_INTERVAL = 1;

	private ItemReader<? extends I> reader;

	private ItemWriter<? super O> writer;

	private ItemProcessor<? super I, ? extends O> processor;

	private int chunkSize = 0;

	private RepeatOperations chunkOperations;

	private CompletionPolicy completionPolicy;

	private Set<StepListener> itemListeners = new LinkedHashSet<StepListener>();

	private boolean readerTransactionalQueue = false;

	public SimpleStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	@Override
	public TaskletStep build() {
		registerAsStreamsAndListeners(reader, processor, writer);
		return super.build();
	}

	@Override
	protected Tasklet createTasklet() {
		Assert.state(reader != null, "ItemReader must be provided");
		Assert.state(processor != null || writer != null, "ItemWriter or ItemProcessor must be provided");
		RepeatOperations repeatOperations = createChunkOperations();
		SimpleChunkProvider<I> chunkProvider = new SimpleChunkProvider<I>(reader, repeatOperations);
		SimpleChunkProcessor<I, O> chunkProcessor = new SimpleChunkProcessor<I, O>(processor, writer);
		chunkProvider.setListeners(new ArrayList<StepListener>(itemListeners));
		chunkProcessor.setListeners(new ArrayList<StepListener>(itemListeners));
		ChunkOrientedTasklet<I> tasklet = new ChunkOrientedTasklet<I>(chunkProvider, chunkProcessor);
		tasklet.setBuffering(!readerTransactionalQueue);
		return tasklet;
	}

	public SimpleStepBuilder<I, O> readerIsTransactionalQueue() {
		this.readerTransactionalQueue = true;
		return this;
	}

	public SimpleStepBuilder<I, O> listener(ItemReadListener<? super I> listener) {
		itemListeners.add(listener);
		return this;
	}

	public SimpleStepBuilder<I, O> listener(ItemWriteListener<? super O> listener) {
		itemListeners.add(listener);
		return this;
	}

	public SimpleStepBuilder<I, O> listener(ItemProcessListener<? super I, ? super O> listener) {
		itemListeners.add(listener);
		return this;
	}

	public SimpleStepBuilder<I, O> chunkOperations(RepeatOperations repeatTemplate) {
		this.chunkOperations = repeatTemplate;
		return this;
	}

	public SimpleStepBuilder<I, O> completionPolicy(CompletionPolicy completionPolicy) {
		Assert.state(chunkSize == 0 || completionPolicy == null,
				"You must specify either a chunkCompletionPolicy or a commitInterval but not both.");
		this.completionPolicy = completionPolicy;
		return this;
	}

	public SimpleStepBuilder<I, O> chunk(int chunkSize) {
		Assert.state(completionPolicy == null || chunkSize == 0,
				"You must specify either a chunkCompletionPolicy or a commitInterval but not both.");
		this.chunkSize = chunkSize;
		return this;
	}

	public SimpleStepBuilder<I, O> chunk(CompletionPolicy completionPolicy) {
		this.completionPolicy = completionPolicy;
		return this;
	}

	public SimpleStepBuilder<I, O> reader(ItemReader<? extends I> reader) {
		this.reader = reader;
		return this;
	}

	public SimpleStepBuilder<I, O> writer(ItemWriter<? super O> writer) {
		this.writer = writer;
		return this;
	}

	public SimpleStepBuilder<I, O> processor(ItemProcessor<? super I, ? extends O> processor) {
		this.processor = processor;
		return this;
	}

	protected RepeatOperations createChunkOperations() {
		RepeatOperations repeatOperations = chunkOperations;
		if (repeatOperations == null) {
			RepeatTemplate repeatTemplate = new RepeatTemplate();
			repeatTemplate.setCompletionPolicy(getChunkCompletionPolicy());
			repeatOperations = repeatTemplate;
		}
		return repeatOperations;
	}

	protected ItemReader<? extends I> getReader() {
		return reader;
	}

	protected ItemWriter<? super O> getWriter() {
		return writer;
	}

	protected ItemProcessor<? super I, ? extends O> getProcessor() {
		return processor;
	}

	protected int getChunkSize() {
		return chunkSize;
	}

	protected boolean isReaderTransactionalQueue() {
		return readerTransactionalQueue;
	}

	protected Set<StepListener> getItemListeners() {
		return itemListeners;
	}

	/**
	 * @return a {@link CompletionPolicy} consistent with the chunk size and injected policy (if present).
	 */
	private CompletionPolicy getChunkCompletionPolicy() {
		Assert.state(!(completionPolicy != null && chunkSize > 0),
				"You must specify either a chunkCompletionPolicy or a commitInterval but not both.");
		Assert.state(chunkSize >= 0, "The commitInterval must be positive or zero (for default value).");

		if (completionPolicy != null) {
			return completionPolicy;
		}
		if (chunkSize == 0) {
			logger.info("Setting commit interval to default value (" + DEFAULT_COMMIT_INTERVAL + ")");
			chunkSize = DEFAULT_COMMIT_INTERVAL;
		}
		return new SimpleCompletionPolicy(chunkSize);
	}

	private void registerAsStreamsAndListeners(ItemReader<? extends I> itemReader,
			ItemProcessor<? super I, ? extends O> itemProcessor, ItemWriter<? super O> itemWriter) {
		for (Object itemHandler : new Object[] { itemReader, itemWriter, itemProcessor }) {
			if (itemHandler instanceof ItemStream) {
				stream((ItemStream) itemHandler);
			}
			if (StepListenerFactoryBean.isListener(itemHandler)) {
				StepListener listener = StepListenerFactoryBean.getListener(itemHandler);
				if (listener instanceof StepExecutionListener) {
					listener((StepExecutionListener) listener);
				}
				if (listener instanceof ChunkListener) {
					listener((ChunkListener) listener);
				}
				if (listener instanceof ItemReadListener<?> || listener instanceof ItemProcessListener<?, ?>
						|| listener instanceof ItemWriteListener<?>) {
					itemListeners.add(listener);
				}
			}
		}
	}

}
