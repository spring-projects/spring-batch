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

package org.springframework.batch.repeat.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * A {@link MethodInterceptor} that can be used to automatically repeat calls to
 * a method on a service. The injected {@link RepeatOperations} is used to
 * control the completion of the loop. Independent of the completion policy in
 * the {@link RepeatOperations} the loop will repeat until the target method
 * returns null or false. Be careful when injecting a bespoke
 * {@link RepeatOperations} that the loop will actually terminate, because the
 * default policy for a vanilla {@link RepeatTemplate} will never complete if
 * the return type of the target method is void (the value returned is always
 * not-null, representing the {@link Void#TYPE}).
 * 
 * @author Dave Syer
 */
public class RepeatOperationsInterceptor implements MethodInterceptor {

	private RepeatOperations repeatOperations = new RepeatTemplate();

	/**
	 * Setter for the {@link RepeatOperations}.
	 * 
	 * @param batchTempate
	 * @throws IllegalArgumentException if the argument is null.
	 */
	public void setRepeatOperations(RepeatOperations batchTempate) {
		Assert.notNull(batchTempate, "'repeatOperations' cannot be null.");
		this.repeatOperations = batchTempate;
	}

	/**
	 * Invoke the proceeding method call repeatedly, according to the properties
	 * of the injected {@link RepeatOperations}.
	 * 
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	public Object invoke(final MethodInvocation invocation) throws Throwable {

		final ResultHolder result = new ResultHolder();
		// Cache void return value if intercepted method returns void
		final boolean voidReturnType = Void.TYPE.equals(invocation.getMethod().getReturnType());
		if (voidReturnType) {
			// This will be ignored anyway, but we want it to be non-null for
			// convenience of checking that there is a result.
			result.setValue(new Object());
		}

		try {
			repeatOperations.iterate(new RepeatCallback() {

				public RepeatStatus doInIteration(RepeatContext context) throws Exception {
					try {

						MethodInvocation clone = invocation;
						if (invocation instanceof ProxyMethodInvocation) {
							clone = ((ProxyMethodInvocation) invocation).invocableClone();
						}
						else {
							throw new IllegalStateException(
									"MethodInvocation of the wrong type detected - this should not happen with Spring AOP, so please raise an issue if you see this exception");
						}

						Object value = clone.proceed();
						if (voidReturnType) {
							return RepeatStatus.CONTINUABLE;
						}
						if (!isComplete(value)) {
							// Save the last result
							result.setValue(value);
							return RepeatStatus.CONTINUABLE;
						}
						else {
							result.setFinalValue(value);
							return RepeatStatus.FINISHED;
						}
					}
					catch (Throwable e) {
						if (e instanceof Exception) {
							throw (Exception) e;
						}
						else {
							throw new RepeatOperationsInterceptorException("Unexpected error in batch interceptor", e);
						}
					}
				}

			});
		}
		catch (Throwable t) {
			// The repeat exception should be unwrapped by the template
			throw t;
		}

		if (result.isReady()) {
			return result.getValue();
		}

		// No result means something weird happened
		throw new IllegalStateException("No result available for attempted repeat call to " + invocation
				+ ".  The invocation was never called, so maybe there is a problem with the completion policy?");
	}

	/**
	 * @param result
	 * @return
	 */
	private boolean isComplete(Object result) {
		return result == null || (result instanceof Boolean) && !((Boolean) result).booleanValue();
	}

	/**
	 * Simple wrapper exception class to enable nasty errors to be passed out of
	 * the scope of the repeat operations and handled by the caller.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class RepeatOperationsInterceptorException extends RepeatException {
		/**
		 * @param message
		 * @param e
		 */
		public RepeatOperationsInterceptorException(String message, Throwable e) {
			super(message, e);
		}
	}

	/**
	 * Simple wrapper object for the result from a method invocation.
	 * 
	 * @author Dave Syer
	 * 
	 */
	private static class ResultHolder {
		private Object value = null;

		private boolean ready = false;

		/**
		 * Public setter for the Object.
		 * @param value the value to set
		 */
		public void setValue(Object value) {
			this.ready = true;
			this.value = value;
		}

		/**
		 * @param value
		 */
		public void setFinalValue(Object value) {
			if (ready) {
				// Only set the value the last time if the last time was also
				// the first time
				return;
			}
			setValue(value);
		}

		/**
		 * Public getter for the Object.
		 * @return the value
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * @return true if a value has been set
		 */
		public boolean isReady() {
			return ready;
		}
	}

}
