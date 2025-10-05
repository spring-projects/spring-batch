/*
 * Copyright 2006-2025 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.springframework.batch.core.ExitStatus;

import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemStream;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 */
public class MulticasterBatchListener<T, S> implements StepExecutionListener, ChunkListener, ItemReadListener<T>,
		ItemProcessListener<T, S>, ItemWriteListener<S>, SkipListener<T, S> {

	private final CompositeStepExecutionListener stepListener = new CompositeStepExecutionListener();

	private final CompositeChunkListener chunkListener = new CompositeChunkListener();

	private final CompositeItemReadListener<T> itemReadListener = new CompositeItemReadListener<>();

	private final CompositeItemProcessListener<T, S> itemProcessListener = new CompositeItemProcessListener<>();

	private final CompositeItemWriteListener<S> itemWriteListener = new CompositeItemWriteListener<>();

	private final CompositeSkipListener<T, S> skipListener = new CompositeSkipListener<>();

	/**
	 * Initialize the listener instance.
	 */
	public MulticasterBatchListener() {
		super();
	}

	/**
	 * Register each of the objects as listeners. Once registered, calls to the
	 * {@link MulticasterBatchListener} broadcast to the individual listeners.
	 * @param listeners listener objects of types known to the multicaster.
	 */
	public void setListeners(List<? extends StepListener> listeners) {
		for (StepListener stepListener : listeners) {
			register(stepListener);
		}
	}

	/**
	 * Register the listener for callbacks on the appropriate interfaces implemented. Any
	 * {@link StepListener} can be provided, or an {@link ItemStream}. Other types will be
	 * ignored.
	 * @param listener the {@link StepListener} instance to be registered.
	 */
	public void register(StepListener listener) {
		if (listener instanceof StepExecutionListener stepExecutionListener) {
			this.stepListener.register(stepExecutionListener);
		}
		if (listener instanceof ChunkListener cl) {
			this.chunkListener.register(cl);
		}
		if (listener instanceof ItemReadListener<?>) {
			@SuppressWarnings("unchecked")
			ItemReadListener<T> itemReadListener = (ItemReadListener<T>) listener;
			this.itemReadListener.register(itemReadListener);
		}
		if (listener instanceof ItemProcessListener<?, ?>) {
			@SuppressWarnings("unchecked")
			ItemProcessListener<T, S> itemProcessListener = (ItemProcessListener<T, S>) listener;
			this.itemProcessListener.register(itemProcessListener);
		}
		if (listener instanceof ItemWriteListener<?>) {
			@SuppressWarnings("unchecked")
			ItemWriteListener<S> itemWriteListener = (ItemWriteListener<S>) listener;
			this.itemWriteListener.register(itemWriteListener);
		}
		if (listener instanceof SkipListener<?, ?>) {
			@SuppressWarnings("unchecked")
			SkipListener<T, S> skipListener = (SkipListener<T, S>) listener;
			this.skipListener.register(skipListener);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeItemProcessListener#afterProcess(java.lang.Object,
	 * java.lang.Object)
	 */
	@Override
	public void afterProcess(T item, @Nullable S result) {
		try {
			itemProcessListener.afterProcess(item, result);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterProcess.", getTargetException(e));
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeItemProcessListener#beforeProcess(java.lang.Object)
	 */
	@Override
	public void beforeProcess(T item) {
		try {
			itemProcessListener.beforeProcess(item);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeProcess.", getTargetException(e));
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeItemProcessListener#onProcessError(java.lang.Object,
	 * java.lang.Exception)
	 */
	@Override
	public void onProcessError(T item, Exception ex) {
		try {
			itemProcessListener.onProcessError(item, ex);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in onProcessError.", e);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#afterStep(StepExecution)
	 */
	@Override
	public @Nullable ExitStatus afterStep(StepExecution stepExecution) {
		try {
			return stepListener.afterStep(stepExecution);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterStep.", e);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#beforeStep(StepExecution)
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
		try {
			stepListener.beforeStep(stepExecution);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeStep.", e);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#afterChunk(ChunkContext
	 * context)
	 */
	@Override
	public void afterChunk(ChunkContext context) {
		try {
			chunkListener.afterChunk(context);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterChunk.", getTargetException(e));
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#beforeChunk(ChunkContext
	 * context)
	 */
	@Override
	public void beforeChunk(ChunkContext context) {
		try {
			chunkListener.beforeChunk(context);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeChunk.", getTargetException(e));
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#afterRead(java.lang.Object)
	 */
	@Override
	public void afterRead(T item) {
		try {
			itemReadListener.afterRead(item);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterRead.", getTargetException(e));
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#beforeRead()
	 */
	@Override
	public void beforeRead() {
		try {
			itemReadListener.beforeRead();
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeRead.", getTargetException(e));
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeItemReadListener#onReadError(java.lang.Exception)
	 */
	@Override
	public void onReadError(Exception ex) {
		try {
			itemReadListener.onReadError(ex);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in onReadError.", e);
		}
	}

	/**
	 * @see ItemWriteListener#afterWrite(Chunk)
	 */
	@Override
	public void afterWrite(Chunk<? extends S> items) {
		try {
			itemWriteListener.afterWrite(items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterWrite.", getTargetException(e));
		}
	}

	/**
	 * @see ItemWriteListener#beforeWrite(Chunk)
	 */
	@Override
	public void beforeWrite(Chunk<? extends S> items) {
		try {
			itemWriteListener.beforeWrite(items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeWrite.", getTargetException(e));
		}
	}

	/**
	 * @see ItemWriteListener#onWriteError(Exception, Chunk)
	 */
	@Override
	public void onWriteError(Exception ex, Chunk<? extends S> items) {
		try {
			itemWriteListener.onWriteError(ex, items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in onWriteError.", e);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeSkipListener#onSkipInRead(java.lang.Throwable)
	 */
	@Override
	public void onSkipInRead(Throwable t) {
		skipListener.onSkipInRead(t);
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeSkipListener#onSkipInWrite(java.lang.Object,
	 * java.lang.Throwable)
	 */
	@Override
	public void onSkipInWrite(S item, Throwable t) {
		skipListener.onSkipInWrite(item, t);
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeSkipListener#onSkipInProcess(Object,
	 * Throwable)
	 */
	@Override
	public void onSkipInProcess(T item, Throwable t) {
		skipListener.onSkipInProcess(item, t);
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		try {
			chunkListener.afterChunkError(context);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterFailedChunk.", e);
		}
	}

	/**
	 * Unwrap the target exception from a wrapped {@link InvocationTargetException}.
	 * @param e the exception to introspect
	 * @return the target exception if any
	 */
	private Throwable getTargetException(RuntimeException e) {
		Throwable cause = e.getCause();
		if (cause instanceof InvocationTargetException invocationTargetException) {
			return invocationTargetException.getTargetException();
		}
		return e;
	}

}
