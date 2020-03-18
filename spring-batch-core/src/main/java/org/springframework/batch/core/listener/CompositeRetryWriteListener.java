/*
 * Copyright 2013 the original author or authors.
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

import java.util.Iterator;
import java.util.List;
import javax.batch.api.chunk.listener.RetryWriteListener;

/**
 * <p>
 * Composite class holding {@link RetryWriteListener}'s.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class CompositeRetryWriteListener implements RetryWriteListener {
	private OrderedComposite<RetryWriteListener> listeners = new OrderedComposite<>();

	/**
	 * <p>
	 * Public setter for the {@link RetryWriteListener}'s.
	 * </p>
	 *
	 * @param listeners the {@link RetryWriteListener}'s to set
	 */
	public void setListeners(List<? extends RetryWriteListener> listeners) {
		this.listeners.setItems(listeners);
	}

	/**
	 * <p>
	 * Register an additional {@link RetryWriteListener}.
	 * </p>
	 *
	 * @param listener the {@link RetryWriteListener} to register
	 */
	public void register(RetryWriteListener listener) {
		listeners.add(listener);
	}

	@Override
	public void onRetryWriteException(List<Object> items, Exception ex) throws Exception {
		for (Iterator<RetryWriteListener> iterator = listeners.reverse(); iterator.hasNext();) {
			RetryWriteListener listener = iterator.next();
			listener.onRetryWriteException(items, ex);
		}
	}
}
