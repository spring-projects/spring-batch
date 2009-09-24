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

package org.springframework.batch.retry.context;

import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.core.AttributeAccessorSupport;

public class RetryContextSupport extends AttributeAccessorSupport implements RetryContext {

	private boolean terminate = false;

	private int count;

	private Throwable lastException;

	private RetryContext parent;

	public RetryContextSupport(RetryContext parent) {
		super();
		this.parent = parent;
	}

	public RetryContext getParent() {
		return this.parent;
	}

	public boolean isExhaustedOnly() {
		return terminate;
	}

	public void setExhaustedOnly() {
		terminate = true;
	}

	public int getRetryCount() {
		return count;
	}

	public Throwable getLastThrowable() {
		return lastException;
	}

	/**
	 * Set the exception for the public interface {@link RetryContext}, and
	 * also increment the retry count if the throwable is non-null.<br/>
	 * 
	 * All {@link RetryPolicy} implementations should use this method when they
	 * register the throwable. It should only be called once per retry attempt
	 * because it increments a counter.<br/>
	 * 
	 * Use of this method is not enforced by the framework - it is a service
	 * provider contract for authors of policies.
	 * 
	 * @param throwable the exception that caused the current retry attempt to
	 * fail.
	 */
	public void registerThrowable(Throwable throwable) {
		this.lastException = throwable;
		if (throwable != null)
			count++;
	}
	
	@Override
	public String toString() {
		return String.format("[RetryContext: count=%d, lastException=%s, exhausted=%b]", count, lastException, terminate);
	}

}
