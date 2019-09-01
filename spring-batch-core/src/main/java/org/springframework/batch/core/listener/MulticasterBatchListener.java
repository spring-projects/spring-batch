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
package org.springframework.batch.core.listener;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.operations.BatchRuntimeException;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 */
public class MulticasterBatchListener<T, S> implements StepExecutionListener, ChunkListener, ItemReadListener<T>,
ItemProcessListener<T, S>, ItemWriteListener<S>, SkipListener<T, S>, RetryReadListener, RetryProcessListener, RetryWriteListener {

	private CompositeStepExecutionListener stepListener = new CompositeStepExecutionListener();

	private CompositeChunkListener chunkListener = new CompositeChunkListener();

	private CompositeItemReadListener<T> itemReadListener = new CompositeItemReadListener<>();

	private CompositeItemProcessListener<T, S> itemProcessListener = new CompositeItemProcessListener<>();

	private CompositeItemWriteListener<S> itemWriteListener = new CompositeItemWriteListener<>();

	private CompositeSkipListener<T, S> skipListener = new CompositeSkipListener<>();

	private CompositeRetryReadListener retryReadListener = new CompositeRetryReadListener();

	private CompositeRetryProcessListener retryProcessListener = new CompositeRetryProcessListener();

	private CompositeRetryWriteListener retryWriteListener = new CompositeRetryWriteListener();

	/**
	 * Initialize the listener instance.
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
	 *
	 * @param listener the {@link StepListener} instance to be registered.
	 */
	public void register(StepListener listener) {
		if (listener instanceof StepExecutionListener) {
			this.stepListener.register((StepExecutionListener) listener);
		}
		if (listener instanceof ChunkListener) {
			this.chunkListener.register((ChunkListener) listener);
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
		if(listener instanceof RetryReadListener) {
			this.retryReadListener.register((RetryReadListener) listener);
		}
		if(listener instanceof RetryProcessListener) {
			this.retryProcessListener.register((RetryProcessListener) listener);
		}
		if(listener instanceof RetryWriteListener) {
			this.retryWriteListener.register((RetryWriteListener) listener);
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
	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		try {
			return stepListener.afterStep(stepExecution);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterStep.", e);
		}
	}

	/**
	 * @see org.springframework.batch.core.listener.CompositeStepExecutionListener#beforeStep(org.springframework.batch.core.StepExecution)
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
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#afterChunk(ChunkContext context)
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
	 * @see org.springframework.batch.core.listener.CompositeChunkListener#beforeChunk(ChunkContext context)
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
	 * @see ItemWriteListener#afterWrite(List)
	 */
	@Override
	public void afterWrite(List<? extends S> items) {
		try {
			itemWriteListener.afterWrite(items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in afterWrite.", getTargetException(e));
		}
	}

	/**
	 * @see ItemWriteListener#beforeWrite(List)
	 */
	@Override
	public void beforeWrite(List<? extends S> items) {
		try {
			itemWriteListener.beforeWrite(items);
		}
		catch (RuntimeException e) {
			throw new StepListenerFailedException("Error in beforeWrite.", getTargetException(e));
		}
	}

	/**
	 * @see ItemWriteListener#onWriteError(Exception, List)
	 */
	@Override
	public void onWriteError(Exception ex, List<? extends S> items) {
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

	@Override
	public void onRetryReadException(Exception ex) throws Exception {
		try {
			retryReadListener.onRetryReadException(ex);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void onRetryProcessException(Object item, Exception ex) throws Exception {
		try {
			retryProcessListener.onRetryProcessException(item, ex);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Override
	public void onRetryWriteException(List<Object> items, Exception ex) throws Exception {
		try {
			retryWriteListener.onRetryWriteException(items, ex);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	/**
	 * Unwrap the target exception from a wrapped {@link InvocationTargetException}.
	 * @param e the exception to introspect
	 * @return the target exception if any
	 */
	private Throwable getTargetException(RuntimeException e) {
		Throwable cause = e.getCause();
		if (cause != null && cause instanceof InvocationTargetException) {
			return ((InvocationTargetException) cause).getTargetException();
		}
		return e;
	}
}
