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

package org.springframework.batch.retry.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemKeyGenerator;
import org.springframework.batch.item.ItemRecoverer;
import org.springframework.batch.item.NewItemIdentifier;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.callback.RecoveryRetryCallback;
import org.springframework.batch.retry.policy.RecoveryCallbackRetryPolicy;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public class StatefulRetryOperationsInterceptor implements MethodInterceptor {

	private transient Log logger = LogFactory.getLog(getClass());

	private ItemKeyGenerator keyGenerator;

	private ItemRecoverer recoverer;

	private NewItemIdentifier newItemIdentifier;

	private final RetryTemplate retryTemplate = new RetryTemplate();

	/**
	 * 
	 */
	public StatefulRetryOperationsInterceptor() {
		super();
		retryTemplate.setRetryPolicy(new RecoveryCallbackRetryPolicy(new NeverRetryPolicy()));
	}

	/**
	 * Public setter for the {@link ItemRecoverer} to use if the retry is
	 * exhausted. The recoverer should be able to return an object of the same
	 * type as the target object because its return value will be used to return
	 * to the caller in the case of a recovery.
	 * 
	 * @param recoverer the {@link ItemRecoverer} to set
	 */
	public void setRecoverer(ItemRecoverer recoverer) {
		this.recoverer = recoverer;
	}

	public void setKeyGenerator(ItemKeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Public setter for the retryPolicy.
	 * @param retryPolicy the retryPolicy to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		retryTemplate.setRetryPolicy(new RecoveryCallbackRetryPolicy(retryPolicy));
	}

	/**
	 * Public setter for the {@link NewItemIdentifier}. Only set this if the
	 * arguments to the intercepted method can be inspected to find out if they
	 * have never been processed before.
	 * @param newItemIdentifier the {@link NewItemIdentifier} to set
	 */
	public void setNewItemIdentifier(NewItemIdentifier newItemIdentifier) {
		this.newItemIdentifier = newItemIdentifier;
	}

	/**
	 * Wrap the method invocation in a stateful retry with the policy and other
	 * helpers provided. If there is a failure the exception will generally be
	 * re-thrown. The only time it is not re-thrown is when retry is exhausted
	 * and the recovery path is taken (though the {@link ItemRecoverer} provided
	 * if there is one). In that case the value returned from the method
	 * invocation will be null, or if primitive then "0" (e.g. Boolean.FALSE, 0L
	 * etc.).
	 * 
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	public Object invoke(final MethodInvocation invocation) throws Throwable {

		logger.debug("Executing proxied method in stateful retry: " + invocation.getStaticPart() + "("
				+ ObjectUtils.getIdentityHexString(invocation) + ")");

		Object[] args = invocation.getArguments();
		Assert.state(args.length > 0, "Stateful retry applied to method that takes no arguments: "
				+ invocation.getStaticPart());
		Object arg = args;
		if (args.length == 1) {
			arg = args[0];
		}
		final Object item = arg;

		RecoveryRetryCallback callback = new RecoveryRetryCallback(item, new RetryCallback() {
			public Object doWithRetry(RetryContext context) throws Throwable {
				return invocation.proceed();
			}
		}, keyGenerator != null ? keyGenerator.getKey(item) : item);
		callback.setRecoveryCallback(new RecoveryCallback() {
			public Object recover(Throwable cause) {
				if (recoverer != null) {
					return recoverer.recover(item, cause);
				}
				return item;
			}
		});
		if (newItemIdentifier != null) {
			callback.setForceRefresh(newItemIdentifier.isNew(item));
		}

		Object result = retryTemplate.execute(callback);

		logger.debug("Exiting proxied method in stateful retry with result: (" + result + ")");

		return result;

	}

}
