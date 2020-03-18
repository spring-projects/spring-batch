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
import javax.batch.api.chunk.listener.RetryReadListener;

/**
 * <p>
 * Composite class holding {@link RetryReadListener}'s.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class CompositeRetryReadListener implements RetryReadListener {
	private OrderedComposite<RetryReadListener> listeners = new OrderedComposite<>();

	/**
	 * <p>
	 * Public setter for the {@link RetryReadListener}'s.
	 * </p>
	 *
	 * @param listeners the {@link RetryReadListener}'s to set
	 */
	public void setListeners(List<? extends RetryReadListener> listeners) {
		this.listeners.setItems(listeners);
	}

	/**
	 * <p>
	 * Register an additional {@link RetryReadListener}.
	 * </p>
	 *
	 * @param listener the {@link RetryReadListener} to register
	 */
	public void register(RetryReadListener listener) {
		listeners.add(listener);
	}

	@Override
	public void onRetryReadException(Exception ex) throws Exception {
		for (Iterator<RetryReadListener> iterator = listeners.reverse(); iterator.hasNext();) {
			RetryReadListener listener = iterator.next();
			listener.onRetryReadException(ex);
		}
	}
}
