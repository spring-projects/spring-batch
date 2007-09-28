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
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.exception.RepeatException;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @since 2.1
 */
public class RepeatOperationsInterceptor implements MethodInterceptor {

	private RepeatOperations batchTempate = new RepeatTemplate();

	/**
	 * Setter for the {@link RepeatOperations}.
	 * @param batchTempate
	 * @throws IllegalArgumentException if the argument is null.
	 */
	public void setRepeatOperations(RepeatOperations batchTempate) {
		Assert.notNull(batchTempate, "'batchTemplate' cannot be null.");
		this.batchTempate = batchTempate;
	}

	/**
	 * Invoke the proceeding method call repeatedly, according to the properties
	 * of the injected {@link RepeatOperations}.
	 * 
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	public Object invoke(final MethodInvocation invocation) throws Throwable {

		batchTempate.iterate(new RepeatCallback() {

			public ExitStatus doInIteration(RepeatContext context) throws Exception {
				try {

					MethodInvocation clone = invocation;
					if (invocation instanceof ReflectiveMethodInvocation) {
						clone = ((ReflectiveMethodInvocation) invocation)
								.invocableClone();
					}
					
					// N.B. discards return value if there is one
					return new ExitStatus(clone.proceed() != null);
				}
				catch (Throwable e) {
					if (e instanceof Exception) {
						throw (Exception) e;
					}
					else {
						throw new RepeatException("Unexpected error in batch interceptor", e);
					}
				}
			}

		});
		
		return null;
	}

}
