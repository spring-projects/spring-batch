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

package org.springframework.batch.retry.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * @author Rob Harrop
 * @author Dave Syer
 * @since 2.1
 */
public class RetryOperationsInterceptor implements MethodInterceptor {

	private RetryOperations retryTemplate = new RetryTemplate();

	public void setRetryTemplate(RetryOperations retryTemplate) {
		Assert.notNull(retryTemplate, "'retryTemplate' cannot be null.");
		this.retryTemplate = retryTemplate;
	}

	public Object invoke(final MethodInvocation invocation) throws Throwable {
		// TODO: use the method name to initialise a statistics context
		return this.retryTemplate.execute(new RetryCallback() {

			public Object doWithRetry(RetryContext context) throws Throwable {

				/*
				 * If we don't copy the invocation carefully it won't keep a
				 * reference to the other interceptors in the chain. We don't
				 * have a choice here but to specialise to
				 * ReflectiveMethodInvocation (but how often would another
				 * implementation come along?).
				 */
				MethodInvocation clone = invocation;
				if (invocation instanceof ReflectiveMethodInvocation) {
					clone = ((ReflectiveMethodInvocation) invocation)
							.invocableClone();
				}

				return clone.proceed();
			}

		});
	}
}
