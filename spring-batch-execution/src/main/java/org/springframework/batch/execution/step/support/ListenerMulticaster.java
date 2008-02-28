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

import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepListener;
import org.springframework.batch.core.interceptor.CompositeStepListener;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.exception.StreamException;
import org.springframework.batch.item.stream.CompositeItemStream;
import org.springframework.batch.repeat.ExitStatus;

/**
 * @author Dave Syer
 * 
 */
public class ListenerMulticaster implements ItemStream, StepListener {

	private CompositeItemStream stream = new CompositeItemStream();

	private CompositeStepListener stepListener = new CompositeStepListener();

	/**
	 * Register each of the objects as listeners. Once registered, calls to the
	 * {@link ListenerMulticaster} broadcast to the individual listeners.
	 * 
	 * @param listeners an array of listener objects of types known to the
	 * multicaster.
	 */
	public void setListeners(Object[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			register(listeners[i]);
		}
	}

	/**
	 * Register the listener for callbacks on the appropriate interfaces
	 * implemented.
	 */
	public void register(Object listener) {
		if (listener instanceof StepListener) {
			this.stepListener.register((StepListener) listener);
		}
		if (listener instanceof ItemStream) {
			this.stream.register((ItemStream) listener);
		}
	}

	/**
	 * @return
	 * @see org.springframework.batch.core.interceptor.CompositeStepListener#afterStep()
	 */
	public ExitStatus afterStep() {
		return stepListener.afterStep();
	}

	/**
	 * @param stepExecution
	 * @see org.springframework.batch.core.interceptor.CompositeStepListener#beforeStep(org.springframework.batch.core.domain.StepExecution)
	 */
	public void beforeStep(StepExecution stepExecution) {
		stepListener.beforeStep(stepExecution);
	}

	/**
	 * @param e
	 * @return
	 * @see org.springframework.batch.core.interceptor.CompositeStepListener#onErrorInStep(java.lang.Throwable)
	 */
	public ExitStatus onErrorInStep(Throwable e) {
		return stepListener.onErrorInStep(e);
	}

	/**
	 * @param executionContext
	 * @throws StreamException
	 * @see org.springframework.batch.item.stream.CompositeItemStream#close(org.springframework.batch.item.ExecutionContext)
	 */
	public void close(ExecutionContext executionContext) throws StreamException {
		stream.close(executionContext);
	}

	/**
	 * @param executionContext
	 * @throws StreamException
	 * @see org.springframework.batch.item.stream.CompositeItemStream#open(org.springframework.batch.item.ExecutionContext)
	 */
	public void open(ExecutionContext executionContext) throws StreamException {
		stream.open(executionContext);
	}

	/**
	 * @param executionContext
	 * @see org.springframework.batch.item.stream.CompositeItemStream#update(org.springframework.batch.item.ExecutionContext)
	 */
	public void update(ExecutionContext executionContext) {
		stream.update(executionContext);
	}

}
