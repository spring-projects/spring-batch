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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.domain.BatchListener;
import org.springframework.batch.core.domain.ChunkListener;
import org.springframework.batch.core.domain.ItemReadListener;
import org.springframework.batch.core.domain.ItemWriteListener;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.execution.listener.CompositeChunkListener;
import org.springframework.batch.execution.listener.CompositeItemReadListener;
import org.springframework.batch.execution.listener.CompositeItemWriteListener;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.reader.DelegatingItemReader;
import org.springframework.batch.item.writer.DelegatingItemWriter;
import org.springframework.batch.repeat.ExitStatus;
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
class BatchListenerFactoryHelper {

	/**
	 * @param itemReader2
	 * @param listeners
	 * @return
	 */
	public ItemReader getItemReader(ItemReader itemReader, BatchListener[] listeners) {

		final CompositeItemReadListener multicaster = new CompositeItemReadListener();

		for (int i = 0; i < listeners.length; i++) {
			BatchListener listener = listeners[i];
			if (listener instanceof ItemReadListener) {
				multicaster.register((ItemReadListener) listener);
			}
		}

		itemReader = new DelegatingItemReader(itemReader) {
			public Object read() throws Exception {
				try {
					multicaster.beforeRead();
					Object item = super.read();
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
	 * @param itemWriter2
	 * @param listeners
	 * @return
	 */
	public ItemWriter getItemWriter(ItemWriter itemWriter, BatchListener[] listeners) {
		final CompositeItemWriteListener multicaster = new CompositeItemWriteListener();

		for (int i = 0; i < listeners.length; i++) {
			BatchListener listener = listeners[i];
			if (listener instanceof ItemWriteListener) {
				multicaster.register((ItemWriteListener) listener);
			}
		}

		itemWriter = new DelegatingItemWriter(itemWriter) {
			public void write(Object item) throws Exception {
				try {
					multicaster.beforeWrite(item);
					super.write(item);
					multicaster.afterWrite();
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
	 * @param stepOperations
	 * @param listeners
	 * @return
	 */
	public RepeatOperations addChunkListeners(RepeatOperations stepOperations, BatchListener[] listeners) {

		final CompositeChunkListener multicaster = new CompositeChunkListener();

		boolean hasChunkListener = false;

		for (int i = 0; i < listeners.length; i++) {
			BatchListener listener = listeners[i];
			if (listener instanceof ChunkListener) {
				hasChunkListener = true;
			}
			if (listener instanceof ChunkListener) {
				multicaster.register((ChunkListener) listener);
			}
		}

		if (hasChunkListener) {

			Assert.state(stepOperations instanceof RepeatTemplate,
					"Step operations is injected but not a RepeatTemplate, so chunk listeners cannot also be registered. "
							+ "Either inject a RepeatTemplate, or remove the ChunkListener.");

			RepeatTemplate stepTemplate = (RepeatTemplate) stepOperations;
			stepTemplate.registerListener(new RepeatListenerSupport() {
				public void before(RepeatContext context) {
					multicaster.beforeChunk();
				}
				public void after(RepeatContext context, ExitStatus result) {
					multicaster.afterChunk();
				}
			});

		}

		return stepOperations;

	}

	/**
	 * @param listeners
	 * @return
	 */
	public StepListener[] getStepListeners(BatchListener[] listeners) {
		List list = new ArrayList();
		for (int i = 0; i < listeners.length; i++) {
			BatchListener listener = listeners[i];
			if (listener instanceof StepListener) {
				list.add(listener);
			}
		}
		return (StepListener[]) list.toArray(new StepListener[list.size()]);
	}

}
