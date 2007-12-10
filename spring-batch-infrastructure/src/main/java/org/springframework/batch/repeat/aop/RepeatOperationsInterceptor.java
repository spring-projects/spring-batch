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

package org.springframework.batch.repeat.aop;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.RepeatException;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * A {@link MethodInterceptor} that can be used to automatically repeat calls to
 * a method on a service. The injected {@link RepeatOperations} is used to
 * control the completion of the loop. By default it will repeat until the
 * target method returns null. Be careful when injecting a bespoke
 * {@link RepeatOperations} that the loop will actually terminate, because the
 * default policy for a vanilla {@link RepeatTemplate} will never complete if
 * the return type of the target method is void (the value returned is always
 * not-null, representing the {@link Void#TYPE}).
 * 
 * @author Dave Syer
 * @since 2.1
 */
public class RepeatOperationsInterceptor implements MethodInterceptor {

	private RepeatOperations repeatOperations = new RepeatTemplate();

	/**
	 * Setter for the {@link RepeatOperations}.
	 * 
	 * @param batchTempate
	 * @throws IllegalArgumentException
	 *             if the argument is null.
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

		repeatOperations.iterate(new RepeatCallback() {

			public ExitStatus doInIteration(RepeatContext context)
					throws Exception {
				try {

					MethodInvocation clone = invocation;
					if (invocation instanceof ProxyMethodInvocation) {
						clone = ((ProxyMethodInvocation) invocation)
								.invocableClone();
					} else {
						throw new IllegalStateException(
								"MethodInvocation of the wrong type detected - this should not happen with Spring AOP, so please raise an issue if you see this exception");
					}

					// N.B. discards return value if there is one
					if (clone.getMethod().getGenericReturnType().equals(
							Void.TYPE)) {
						clone.proceed();
						return ExitStatus.CONTINUABLE;
					}
					return new ExitStatus(clone.proceed() != null);
				} catch (Throwable e) {
					if (e instanceof Exception) {
						throw (Exception) e;
					} else {
						throw new RepeatException(
								"Unexpected error in batch interceptor", e);
					}
				}
			}

		});

		return null;
	}

}
