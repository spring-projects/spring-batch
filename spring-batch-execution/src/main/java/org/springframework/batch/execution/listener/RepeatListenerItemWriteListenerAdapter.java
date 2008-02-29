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
package org.springframework.batch.execution.listener;

import org.springframework.batch.core.domain.ItemWriteListener;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;

/**
 * Adapts a {@link RepeatListener} to the {@link ItemWriteListener} interface.
 * The {@link RepeatListener} is assumed to be targeted at the chunk operations
 * in a step, so after an item corresponds to the after method in
 * {@link RepeatListener}.<br/>
 * 
 * A {@link RepeatContext} is obtained as needed from the
 * {@link RepeatSynchronizationManager}.
 * 
 * @author Dave Syer
 * 
 */
public class RepeatListenerItemWriteListenerAdapter implements ItemWriteListener {

	private RepeatListener delegate;

	/**
	 * @param delegate
	 */
	public RepeatListenerItemWriteListenerAdapter(RepeatListener delegate) {
		super();
		this.delegate = delegate;
	}

	/**
	 * Calls the delegate {@link RepeatListener#after} with
	 * {@link ExitStatus#CONTINUABLE}.
	 * 
	 * @see org.springframework.batch.core.domain.ItemWriteListener#afterWrite()
	 */
	public void afterWrite() {
		delegate.after(RepeatSynchronizationManager.getContext(), ExitStatus.CONTINUABLE);
	}

	/**
	 * Does nothing.
	 * 
	 * @see org.springframework.batch.core.domain.ItemWriteListener#beforeWrite(java.lang.Object)
	 */
	public void beforeWrite(Object item) {
		// NO-OP
	}

	/**
	 * Calls the delegate {@link RepeatListener#onError} ignoring the item.
	 * 
	 * @see org.springframework.batch.core.domain.ItemWriteListener#onWriteError(java.lang.Exception,
	 * java.lang.Object)
	 */
	public void onWriteError(Exception ex, Object item) {
		delegate.onError(RepeatSynchronizationManager.getContext(), ex);
	}

}
