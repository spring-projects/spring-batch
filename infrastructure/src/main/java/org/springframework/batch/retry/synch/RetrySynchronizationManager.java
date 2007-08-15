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

package org.springframework.batch.retry.synch;

import org.springframework.batch.retry.RetryContext;

public class RetrySynchronizationManager {

	private RetrySynchronizationManager() {
	}

	private static final ThreadLocal context = new ThreadLocal();

	public static RetryContext getContext() {
		RetryContext result = (RetryContext) context.get();
		return result;
	}

	public static RetryContext register(RetryContext context) {
		RetryContext oldContext = getContext();
		RetrySynchronizationManager.context.set(context);
		return oldContext;
	}

	public static RetryContext clear() {
		RetryContext value = getContext();
		RetryContext parent = value == null ? null : value.getParent();
		RetrySynchronizationManager.context.set(parent);
		return value;
	}

	public static RetryContext clearAll() {
		RetryContext result = null;
		RetryContext context = clear();
		while (context != null) {
			result = context;
			context = clear();
		}
		return result;
	}

}
