/*
 * Copyright 2006-2008 the original author or authors.
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

import org.springframework.batch.core.domain.BatchListener;
import org.springframework.batch.core.domain.ChunkListener;
import org.springframework.batch.core.domain.ItemReadListener;
import org.springframework.batch.core.domain.ItemWriteListener;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Basic no-op implementations of all {@link BatchListener} implementations.
 * 
 * @author Lucas Ward
 *
 */
public class BatchListenerSupport implements StepListener, ChunkListener,
		ItemReadListener, ItemWriteListener {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#afterStep(StepExecution stepExecution)
	 */
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#beforeStep(org.springframework.batch.core.domain.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepListener#onErrorInStep(java.lang.Throwable)
	 */
	public ExitStatus onErrorInStep(StepExecution stepExecution, Throwable e) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#afterChunk()
	 */
	public void afterChunk() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ChunkListener#beforeChunk()
	 */
	public void beforeChunk() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#afterRead(java.lang.Object)
	 */
	public void afterRead(Object item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#beforeRead()
	 */
	public void beforeRead() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemReadListener#onReadError(java.lang.Exception)
	 */
	public void onReadError(Exception ex) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemWriteListener#afterWrite()
	 */
	public void afterWrite() {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemWriteListener#beforeWrite(java.lang.Object)
	 */
	public void beforeWrite(Object item) {
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.ItemWriteListener#onWriteError(java.lang.Exception, java.lang.Object)
	 */
	public void onWriteError(Exception ex, Object item) {
	}

}
