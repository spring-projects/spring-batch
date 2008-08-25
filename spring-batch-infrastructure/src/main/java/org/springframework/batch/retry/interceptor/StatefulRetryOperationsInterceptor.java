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
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RecoveryCallback;
import org.springframework.batch.retry.RetryCallback;
import org.springframework.batch.retry.RetryContext;
import org.springframework.batch.retry.RetryOperations;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.RetryState;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A {@link MethodInterceptor} that can be used to automatically retry calls to
 * a method on a service if it fails. The argument to the service method is
 * treated as an item to be remembered in case the call fails. So the retry
 * operation is stateful, and the item that failed is tracked by its unique key
 * (via {@link ItemKeyGenerator}) until the retry is exhausted, at which point
 * the {@link ItemRecoverer} is called.<br/>
 * 
 * The main use case for this is where the service is transactional, via a
 * transaction interceptor on the interceptor chain. In this case the retry (and
 * recovery on exhausted) always happens in a new transaction.<br/>
 * 
 * The injected {@link RetryOperations} is used to control the number of
 * retries. By default it will retry a fixed number of times, according to the
 * defaults in {@link RetryTemplate}.<br/>
 * 
 * @author Dave Syer
 */
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
		retryTemplate.setRetryPolicy(new NeverRetryPolicy());
	}

	/**
	 * Public setter for the {@link ItemRecoverer} to use if the retry is
	 * exhausted. The recoverer should be able to return an object of the same
	 * type as the target object because its return value will be used to return
	 * to the caller in the case of a recovery.<br/>
	 * 
	 * If no recoverer is set then an exhausted retry will result in an
	 * {@link ExhaustedRetryException}.
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
	 * Public setter for the retryPolicy. The value provided should be a normal
	 * stateless policy, which is wrapped into a stateful policy inside this
	 * method.
	 * @param retryPolicy the retryPolicy to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		retryTemplate.setRetryPolicy(retryPolicy);
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
	 * invocation will be the value returned by the recoverer (so the return
	 * type for that should be the same as the intercepted method).
	 * 
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 * @see ItemRecoverer#recover(Object, Throwable)
	 * 
	 * @throws ExhaustedRetryException if the retry is exhausted and no
	 * {@link ItemRecoverer} is provided.
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

		RetryState retryState = new RetryState(keyGenerator != null ? keyGenerator.getKey(item) : item, newItemIdentifier != null ? newItemIdentifier.isNew(item) : false );

		Object result = retryTemplate.execute(new MethodInvocationRetryCallback(invocation), new ItemRecovererCallback(item, recoverer), retryState);

		logger.debug("Exiting proxied method in stateful retry with result: (" + result + ")");

		return result;

	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static final class MethodInvocationRetryCallback implements RetryCallback {
		/**
		 * 
		 */
		private final MethodInvocation invocation;

		/**
		 * @param invocation
		 */
		private MethodInvocationRetryCallback(MethodInvocation invocation) {
			this.invocation = invocation;
		}

		public Object doWithRetry(RetryContext context) throws Exception {
			try {
				return invocation.proceed();
			}
			catch (Exception e) {
				throw e;
			}
			catch (Error e) {
				throw e;
			}
			catch (Throwable e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/**
	 * @author Dave Syer
	 * 
	 */
	private static final class ItemRecovererCallback implements RecoveryCallback {

		private final Object item;

		private final ItemRecoverer recoverer;

		/**
		 * @param item the item that failed.
		 */
		private ItemRecovererCallback(Object item, ItemRecoverer recoverer) {
			this.item = item;
			this.recoverer = recoverer;
		}

		public Object recover(RetryContext context) {
			if (recoverer != null) {
				return recoverer.recover(item, context.getLastThrowable());
			}
			throw new ExhaustedRetryException("Retry was exhausted but there was no recovery path.");
		}

	}

}
