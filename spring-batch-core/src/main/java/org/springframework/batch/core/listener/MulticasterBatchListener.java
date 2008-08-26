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

import java.util.List;

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
public class MulticasterBatchListener<T, S> implements StepExecutionListener, ChunkListener, ItemReadListener<T>,
		ItemWriteListener<S>, SkipListener<S> {

	private CompositeStepExecutionListener stepListener = new CompositeStepExecutionListener();

	private CompositeChunkListener chunkListener = new CompositeChunkListener();

	private CompositeItemReadListener<T> itemReadListener = new CompositeItemReadListener<T>();

	private CompositeItemWriteListener<S> itemWriteListener = new CompositeItemWriteListener<S>();

	private CompositeSkipListener<S> skipListener = new CompositeSkipListener<S>();

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
	 * @param listeners listener objects of types known to the multicaster.
	 */
	public void setListeners(List<? extends StepListener> listeners) {
		for (StepListener stepListener : listeners) {
			register(stepListener);
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
			// TODO: make this type safe somehow?
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
		try {
			return stepListener.afterStep(stepExecution);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterStep.", e);
		}
	}

	/**
	 * @param stepExecution
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#beforeStep(org.springframework.batch.core.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		try {
			stepListener.beforeStep(stepExecution);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeStep.", e);
		}
	}

	/**
	 * @param t
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#onErrorInStep(StepExecution,
	 * Throwable)
	 */
	public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable t) {
		try {
			return stepListener.onErrorInStep(stepExecution, t);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in onErrorInStep.", t, e);
		}
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#afterChunk()
	 */
	public void afterChunk() {
		try {
			chunkListener.afterChunk();
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterChunk.", e);
		}
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#beforeChunk()
	 */
	public void beforeChunk() {
		try {
			chunkListener.beforeChunk();
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeChunk.", e);
		}
	}

	/**
	 * @param item
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#afterRead(java.lang.Object)
	 */
	public void afterRead(T item) {
		try {
			itemReadListener.afterRead(item);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterRead.", e);
		}
	}

	/**
	 * 
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#beforeRead()
	 */
	public void beforeRead() {
		try {
			itemReadListener.beforeRead();
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeRead.", e);
		}
	}

	/**
	 * @param ex
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#onReadError(java.lang.Exception)
	 */
	public void onReadError(Exception ex) {
		try {
			itemReadListener.onReadError(ex);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in onReadError.", ex, e);
		}
	}

	/**
	 * 
	 * @see ItemWriteListener#afterWrite(List)
	 */
	public void afterWrite(List<? extends S> items) {
		try {
			itemWriteListener.afterWrite(items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterWrite.", e);
		}
	}

	/**
	 * @param items
	 * @see ItemWriteListener#beforeWrite(List)
	 */
	public void beforeWrite(List<? extends S> items) {
		try {
			itemWriteListener.beforeWrite(items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeWrite.", e);
		}
	}

	/**
	 * @param ex
	 * @param items
	 * @see ItemWriteListener#onWriteError(Exception, List)
	 */
	public void onWriteError(Exception ex, List<? extends S> items) {
		try {
			itemWriteListener.onWriteError(ex, items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in onWriteError.", ex, e);
		}
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
	 * @see org.springframework.batch.core.listener.CompositeSkipListener#onSkipInWrite(java.lang.Object,
	 * java.lang.Throwable)
	 */
	public void onSkipInWrite(S item, Throwable t) {
		skipListener.onSkipInWrite(item, t);
	}

}
