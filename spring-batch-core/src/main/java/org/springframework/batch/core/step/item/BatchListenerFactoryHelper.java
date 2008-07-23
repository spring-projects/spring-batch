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
import java.util.List;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.CompositeChunkListener;
import org.springframework.batch.core.listener.CompositeItemReadListener;
import org.springframework.batch.core.listener.CompositeItemWriteListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.DelegatingItemReader;
import org.springframework.batch.item.support.DelegatingItemWriter;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.listener.RepeatListenerSupport;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * Package private helper for step factory beans.
 * 
 * @author Dave Syer
 * 
 */
abstract class BatchListenerFactoryHelper {

	/**
	 * @param itemReader
	 * @param listeners
	 */
	public static <T> ItemReader<T> getItemReader(ItemReader<T> itemReader, StepListener[] listeners) {

		final CompositeItemReadListener multicaster = new CompositeItemReadListener();

		for (int i = 0; i < listeners.length; i++) {
			StepListener listener = listeners[i];
			if (listener instanceof ItemReadListener) {
				multicaster.register((ItemReadListener) listener);
			}
		}

		itemReader = new DelegatingItemReader<T>(itemReader) {
			public T read() throws Exception {
				try {
					multicaster.beforeRead();
					T item = super.read();
					multicaster.afterRead(item);
					return item;
				}
				catch (Exception e) {
					multicaster.onReadError(e);
					throw e;
				}
			}
		};

		return itemReader;
	}

	/**
	 * @param itemWriter
	 * @param listeners
	 */
	public static <T> ItemWriter<T> getItemWriter(ItemWriter<T> itemWriter, StepListener[] listeners) {
		final CompositeItemWriteListener multicaster = new CompositeItemWriteListener();

		for (int i = 0; i < listeners.length; i++) {
			StepListener listener = listeners[i];
			if (listener instanceof ItemWriteListener) {
				multicaster.register((ItemWriteListener) listener);
			}
		}

		itemWriter = new DelegatingItemWriter<T,T>(itemWriter) {
			public void write(T item) throws Exception {
				try {
					multicaster.beforeWrite(item);
					super.write(item);
					multicaster.afterWrite(item);
				}
				catch (Exception e) {
					multicaster.onWriteError(e, item);
					throw e;
				}
			}
		};

		return itemWriter;

	}

	/**
	 * @param chunkOperations
	 * @param listeners
	 */
	public static RepeatOperations addChunkListeners(RepeatOperations chunkOperations, StepListener[] listeners) {

		final CompositeChunkListener multicaster = new CompositeChunkListener();

		boolean hasChunkListener = false;

		for (int i = 0; i < listeners.length; i++) {
			StepListener listener = listeners[i];
			if (listener instanceof ChunkListener) {
				hasChunkListener = true;
			}
			if (listener instanceof ChunkListener) {
				multicaster.register((ChunkListener) listener);
			}
		}

		if (hasChunkListener) {

			Assert.state(chunkOperations instanceof RepeatTemplate,
					"Chunk operations is injected but not a RepeatTemplate, so chunk listeners cannot also be registered. "
							+ "Either inject a RepeatTemplate, or remove the ChunkListener.");

			RepeatTemplate stepTemplate = (RepeatTemplate) chunkOperations;
			stepTemplate.registerListener(new RepeatListenerSupport() {
				public void open(RepeatContext context) {
					multicaster.beforeChunk();
				}

				public void close(RepeatContext context) {
					multicaster.afterChunk();
				}
			});

		}

		return chunkOperations;

	}

	/**
	 * @param listeners
	 */
	public static StepExecutionListener[] getStepListeners(StepListener[] listeners) {
		List<StepExecutionListener> list = new ArrayList<StepExecutionListener>();
		for (int i = 0; i < listeners.length; i++) {
			StepListener listener = listeners[i];
			if (listener instanceof StepExecutionListener) {
				list.add((StepExecutionListener) listener);
			}
		}
		return list.toArray(new StepExecutionListener[list.size()]);
	}

	/**
	 * @param listeners
	 */
	public static SkipListener[] getSkipListeners(StepListener[] listeners) {
		List<SkipListener> list = new ArrayList<SkipListener>();
		for (int i = 0; i < listeners.length; i++) {
			StepListener listener = listeners[i];
			if (listener instanceof SkipListener) {
				list.add((SkipListener) listener);
			}
		}
		return list.toArray(new SkipListener[list.size()]);
	}

}
