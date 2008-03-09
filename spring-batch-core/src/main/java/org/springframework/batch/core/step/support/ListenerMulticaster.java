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
package org.springframework.batch.core.step.support;

import org.springframework.batch.core.BatchListener;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.CompositeChunkListener;
import org.springframework.batch.core.listener.CompositeItemReadListener;
import org.springframework.batch.core.listener.CompositeItemWriteListener;
import org.springframework.batch.core.listener.CompositeStepListener;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class ListenerMulticaster implements StepListener, ChunkListener, ItemReadListener,
		ItemWriteListener {

	private CompositeStepListener stepListener = new CompositeStepListener();

	private CompositeChunkListener chunkListener = new CompositeChunkListener();

	private CompositeItemReadListener itemReadListener = new CompositeItemReadListener();

	private CompositeItemWriteListener itemWriteListener = new CompositeItemWriteListener();

	/**
	 * Initialise the listener instance.
	 */
	public ListenerMulticaster() {
		super();
	}

	/**
	 * Register each of the objects as listeners. Once registered, calls to the
	 * {@link ListenerMulticaster} broadcast to the individual listeners.
	 * 
	 * @param listeners an array of listener objects of types known to the
	 * multicaster.
	 */
	public void setListeners(BatchListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			register(listeners[i]);
		}
	}

	/**
	 * Register the listener for callbacks on the appropriate interfaces
	 * implemented. Any {@link BatchListener} can be provided, or an
	 * {@link ItemStream}. Other types will be ignored.
	 */
	public void register(BatchListener listener) {
		if (listener instanceof StepListener) {
			this.stepListener.register((StepListener) listener);
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
	}

	/**
	 * @return
	 * @see org.springframework.batch.core.listener.CompositeStepListener#afterStep()
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		return stepListener.afterStep(stepExecution);
	}

	/**
	 * @param stepExecution
	 * @see org.springframework.batch.core.listener.CompositeStepListener#beforeStep(org.springframework.batch.core.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		stepListener.beforeStep(stepExecution);
	}

	/**
	 * @param e
	 * @return
	 * @see org.springframework.batch.core.listener.CompositeStepListener#onErrorInStep(java.lang.Throwable)
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
	 * @see org.springframework.batch.core.listener.CompositeItemWriteListener#afterWrite()
	 */
	public void afterWrite() {
		itemWriteListener.afterWrite();
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

}
