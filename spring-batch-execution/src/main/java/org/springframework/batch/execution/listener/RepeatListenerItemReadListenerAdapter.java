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

import org.springframework.batch.core.domain.ItemReadListener;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.synch.RepeatSynchronizationManager;
import org.springframework.util.ObjectUtils;

/**
 * Adapts a {@link RepeatListener} to the {@link ItemReadListener} interface.
 * The {@link RepeatListener} is assumed to be targeted at the chunk operations
 * in a step, so after an item corresponds to the after method in
 * {@link RepeatListener}.<br/>
 * 
 * The open and close methods on the {@link RepeatListener} are also invoked.
 * The open method is called on the first call to {@link #beforeRead()}, and
 * the close is registered as a destruction callback in the
 * {@link RepeatContext}.<br/>
 * 
 * A {@link RepeatContext} is obtained as needed from the
 * {@link RepeatSynchronizationManager}.
 * 
 * @author Dave Syer
 * 
 */
public class RepeatListenerItemReadListenerAdapter implements ItemReadListener {

	private RepeatListener delegate;

	/**
	 * @param delegate
	 */
	public RepeatListenerItemReadListenerAdapter(RepeatListener delegate) {
		super();
		this.delegate = delegate;
	}

	/**
	 * Does nothing.
	 * 
	 * @see org.springframework.batch.core.domain.ItemReadListener#afterRead(java.lang.Object)
	 */
	public void afterRead(Object item) {
		// NO-OP

	}

	/**
	 * Calls the delegate {@link RepeatListener#before}. Also calls
	 * {@link RepeatListener#open} if it hasn't been called yet on this context.
	 * 
	 * @see org.springframework.batch.core.domain.ItemReadListener#beforeRead()
	 */
	public void beforeRead() {
		RepeatContext context = RepeatSynchronizationManager.getContext();
		maybeOpen(context);
		delegate.before(context);
	}

	/**
	 * @param context
	 */
	private void maybeOpen(final RepeatContext context) {
		String identity = ObjectUtils.identityToString(delegate);
		if (!context.hasAttribute(identity)) {
			context.setAttribute(identity, Boolean.TRUE);
			delegate.open(context);
			context.registerDestructionCallback(identity, new Runnable() {
				public void run() {
					delegate.close(context);
				}
			});
		}
	}

	/**
	 * Calls the delegate {@link RepeatListener#onError}.
	 * 
	 * @see org.springframework.batch.core.domain.ItemReadListener#onReadError(java.lang.Exception)
	 */
	public void onReadError(Exception ex) {
		delegate.onError(RepeatSynchronizationManager.getContext(), ex);
	}

}
