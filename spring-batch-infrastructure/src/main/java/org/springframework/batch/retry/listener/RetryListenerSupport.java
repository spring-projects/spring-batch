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

package org.springframework.batch.retry.listener;

import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryListener;

/**
 * Empty method implementation of {@link RetryListener}.
 * 
 * @author Dave Syer
 *
 */
public class RetryListenerSupport implements RetryListener {

	public <T> void close(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
	}

	public <T> void onError(RetryContext context, RetryCallback<T> callback, Throwable throwable) {
	}

	public <T> boolean open(RetryContext context, RetryCallback<T> callback) {
		return true;
	}

}
