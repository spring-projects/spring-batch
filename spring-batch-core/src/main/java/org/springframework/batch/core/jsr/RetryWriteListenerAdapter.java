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
package org.springframework.batch.core.jsr;

import java.util.List;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.operations.BatchRuntimeException;

/**
 * <p>
 * Wrapper class to adapt a {@link RetryWriteListener} to a {@link RetryListener}.
 * </p>
 *
 * @author Chris Schaefer
 * @since 3.0
 */
public class RetryWriteListenerAdapter implements RetryListener, RetryWriteListener {
	private RetryWriteListener retryWriteListener;

	public RetryWriteListenerAdapter(RetryWriteListener retryWriteListener) {
		this.retryWriteListener = retryWriteListener;
	}

	@Override
	public void onRetryWriteException(List<Object> items, Exception ex) throws Exception {
		try {
			retryWriteListener.onRetryWriteException(items, ex);
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
