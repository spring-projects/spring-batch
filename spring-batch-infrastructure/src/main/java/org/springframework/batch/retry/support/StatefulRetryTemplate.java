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

package org.springframework.batch.retry.support;

import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;

/**
 * 
 * 
 * @author Dave Syer
 */
public class StatefulRetryTemplate extends RetryTemplate {

	/**
	 * @param retryCallback
	 * @param recoveryCallback
	 * @param retryPolicy
	 * @return the result of the callback
	 * @throws Exception
	 */
	protected Object doExecute(RetryCallback retryCallback, RecoveryCallback recoveryCallback, RetryPolicy retryPolicy)
			throws Exception {

		return super.doExecute(retryCallback, recoveryCallback, retryPolicy);

	}

	/**
	 * Extension point for subclasses to decide on behaviour after catching an
	 * exception in a {@link RetryCallback}. Normal stateless behaviour is not
	 * to rethrow.
	 * 
	 * @param context the current {@link RetryContext}
	 * 
	 * @return false but subclasses might choose otherwise
	 */
	protected boolean shouldRethrow(RetryContext context) {
		return false;
	}

}
