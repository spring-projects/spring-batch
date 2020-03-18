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

import java.util.List;

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
import org.springframework.lang.Nullable;

/**
 * Basic no-op implementations of all {@link StepListener} interfaces.
 *
 * @author Lucas Ward
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
public class StepListenerSupport<T,S> implements StepExecutionListener, ChunkListener,
ItemReadListener<T>, ItemProcessListener<T,S>, ItemWriteListener<S>, SkipListener<T, S> {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.StepExecutionListener#afterStep(org.springframework.batch.core.StepExecution)
	 */
	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.StepExecutionListener#beforeStep(org.springframework.batch.core.StepExecution)
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#afterChunk(ChunkContext context)
	 */
	@Override
	public void afterChunk(ChunkContext context) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#beforeChunk(ChunkContext context)
	 */
	@Override
	public void beforeChunk(ChunkContext context) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#afterRead(java.lang.Object)
	 */
	@Override
	public void afterRead(T item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#beforeRead()
	 */
	@Override
	public void beforeRead() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#onReadError(java.lang.Exception)
	 */
	@Override
	public void onReadError(Exception ex) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.ItemWriteListener#afterWrite(java.util.List)
	 */
	@Override
	public void afterWrite(List<? extends S> items) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.ItemWriteListener#beforeWrite(java.util.List)
	 */
	@Override
	public void beforeWrite(List<? extends S> items) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.ItemWriteListener#onWriteError(java.lang.Exception, java.util.List)
	 */
	@Override
	public void onWriteError(Exception exception, List<? extends S> items) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.ItemProcessListener#afterProcess(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void afterProcess(T item, @Nullable S result) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.ItemProcessListener#beforeProcess(java.lang.Object)
	 */
	@Override
	public void beforeProcess(T item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.ItemProcessListener#onProcessError(java.lang.Object, java.lang.Exception)
	 */
	@Override
	public void onProcessError(T item, Exception e) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.SkipListener#onSkipInProcess(java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void onSkipInProcess(T item, Throwable t) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.SkipListener#onSkipInRead(java.lang.Throwable)
	 */
	@Override
	public void onSkipInRead(Throwable t) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.SkipListener#onSkipInWrite(java.lang.Object, java.lang.Throwable)
	 */
	@Override
	public void onSkipInWrite(S item, Throwable t) {
	}

	@Override
	public void afterChunkError(ChunkContext context) {
	}

}
