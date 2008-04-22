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
package org.springframework.batch.core.listener;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class MulticasterBatchListener implements StepExecutionListener, ChunkListener, ItemReadListener,
		ItemWriteListener, SkipListener {

	private CompositeStepExecutionListener stepListener = new CompositeStepExecutionListener();

	private CompositeChunkListener chunkListener = new CompositeChunkListener();

	private CompositeItemReadListener itemReadListener = new CompositeItemReadListener();

	private CompositeItemWriteListener itemWriteListener = new CompositeItemWriteListener();

	private CompositeSkipListener skipListener = new CompositeSkipListener();

	/**
	 * Initialise the listener instance.
	 */
	public MulticasterBatchListener() {
		super();
	}

	/**
	 * Register each of the objects as listeners. Once registered, calls to the
	 * {@link MulticasterBatchListener} broadcast to the individual listeners.
	 * 
	 * @param listeners an array of listener objects of types known to the
	 * multicaster.
	 */
	public void setListeners(StepListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			register(listeners[i]);
		}
	}

	/**
	 * Register the listener for callbacks on the appropriate interfaces
	 * implemented. Any {@link StepListener} can be provided, or an
	 * {@link ItemStream}. Other types will be ignored.
	 */
	public void register(StepListener listener) {
		if (listener instanceof StepExecutionListener) {
			this.stepListener.register((StepExecutionListener) listener);
		}
		if (listener instanceof ChunkListener) {
			this.chunkListener.register((ChunkListener) listener);
		}
		if (listener instanceof ItemReadListener) {
			this.itemReadListener.register((ItemReadListener) listener);
		}
		if (listener instanceof ItemWriteListener) {
			this.itemWriteListener.register((ItemWriteListener) listener);
		}
		if (listener instanceof SkipListener) {
			this.skipListener.register((SkipListener) listener);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#afterStep(StepExecution)
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		return stepListener.afterStep(stepExecution);
	}

	/**
	 * @param stepExecution
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#beforeStep(org.springframework.batch.core.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		stepListener.beforeStep(stepExecution);
	}

	/**
	 * @param e
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#onErrorInStep(StepExecution, Throwable)
	 */
	public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable e) {
		return stepListener.onErrorInStep(stepExecution, e);
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#afterChunk()
	 */
	public void afterChunk() {
		chunkListener.afterChunk();
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#beforeChunk()
	 */
	public void beforeChunk() {
		chunkListener.beforeChunk();
	}

	/**
	 * @param item
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#afterRead(java.lang.Object)
	 */
	public void afterRead(Object item) {
		itemReadListener.afterRead(item);
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#beforeRead()
	 */
	public void beforeRead() {
		itemReadListener.beforeRead();
	}

	/**
	 * @param ex
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#onReadError(java.lang.Exception)
	 */
	public void onReadError(Exception ex) {
		itemReadListener.onReadError(ex);
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeItemWriteListener#afterWrite(Object)
	 */
	public void afterWrite(Object item) {
		itemWriteListener.afterWrite(item);
	}

	/**
	 * @param item
	 * @see org.springframework.batch.core.listener.CompositeItemWriteListener#beforeWrite(java.lang.Object)
	 */
	public void beforeWrite(Object item) {
		itemWriteListener.beforeWrite(item);
	}

	/**
	 * @param ex
	 * @param item
	 * @see org.springframework.batch.core.listener.CompositeItemWriteListener#onWriteError(java.lang.Exception,
	 * java.lang.Object)
	 */
	public void onWriteError(Exception ex, Object item) {
		itemWriteListener.onWriteError(ex, item);
	}

	/**
	 * @param t
	 * @see org.springframework.batch.core.listener.CompositeSkipListener#onSkipInRead(java.lang.Throwable)
	 */
	public void onSkipInRead(Throwable t) {
		skipListener.onSkipInRead(t);
	}

	/**
	 * @param item
	 * @param t
	 * @see org.springframework.batch.core.listener.CompositeSkipListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)
	 */
	public void onSkipInWrite(Object item, Throwable t) {
		skipListener.onSkipInWrite(item, t);
	}

}
