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

package org.springframework.batch.retry.policy;

import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;

/**
 * Base class for "normal" retry policies: those that operate in the context of
 * a callback that is called repeatedly in a loop until it succeeds, or the
 * policy decides to terminate. There is no need for such policies to store
 * state outside the context.
 * 
 * @see RetryPolicy#handleRetryExhausted(RetryContext)
 * @see AbstractStatefulRetryPolicy
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractStatelessRetryPolicy implements RetryPolicy {

	/**
	 * Just returns the negative of {@link RetryPolicy#canRetry(RetryContext)},
	 * i.e. if we cannot retry then the exception should be thrown.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#shouldRethrow(org.springframework.batch.retry.RetryContext)
	 */
	public boolean shouldRethrow(RetryContext context) {
		return !canRetry(context);
	}

	/**
	 * Throw an exception.
	 * 
	 * @see org.springframework.batch.retry.RetryPolicy#handleRetryExhausted(org.springframework.batch.retry.RetryContext)
	 */
	public Object handleRetryExhausted(RetryContext context) throws Exception, ExhaustedRetryException {
		throw new ExhaustedRetryException("Retry exhausted after last attempt with no recovery path.", context
				.getLastThrowable());
	}

}
