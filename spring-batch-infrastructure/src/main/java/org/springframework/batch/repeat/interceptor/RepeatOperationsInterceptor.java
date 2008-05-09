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

import java.util.ArrayList;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatException;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

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

		final List results = new ArrayList();

		try {
			repeatOperations.iterate(new RepeatCallback() {

				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					try {

						MethodInvocation clone = invocation;
						if (invocation instanceof ProxyMethodInvocation) {
							clone = ((ProxyMethodInvocation) invocation).invocableClone();
						}
						else {
							throw new IllegalStateException(
									"MethodInvocation of the wrong type detected - this should not happen with Spring AOP, so please raise an issue if you see this exception");
						}

						// N.B. discards return value if there is one
						if (clone.getMethod().getReturnType().equals(Void.TYPE)) {
							clone.proceed();
							return ExitStatus.CONTINUABLE;
						}
						Object result = clone.proceed();
						if (!isComplete(result)) {
							// We only save the last non-null result
							results.clear();
							results.add(result);
							return ExitStatus.CONTINUABLE;
						}
						else {
							return ExitStatus.FINISHED;
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
		catch (RepeatOperationsInterceptorException e) {
			// Unwrap and re-throw any nasty errors
			throw e.getCause();
		}
		catch (Throwable t) {
			throw t;
		}

		if (!results.isEmpty()) {
			return results.get(0);
		}

		Class returnType = invocation.getMethod().getReturnType();
		Object defaultValue = null;
		if (ClassUtils.isPrimitiveOrWrapper(returnType)) {
			defaultValue = getDefaultForPrimitiveType(returnType);
		}
		return defaultValue;
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
	 * Set up a default return value for primitive types (all basically "0").
	 * @param returnType the desired primitive type
	 * @return a value to use as the default return value if recovery path is
	 * taken
	 */
	// TODO: cache these values.
	private Object getDefaultForPrimitiveType(Class returnType) {
		if (returnType.equals(Boolean.TYPE)) {
			return Boolean.FALSE;
		}
		else if (returnType.equals(Byte.TYPE)) {
			return Byte.valueOf("0");
		}
		else if (returnType.equals(Character.TYPE)) {
			return Character.valueOf('0');
		}
		else if (returnType.equals(Short.TYPE)) {
			return Short.valueOf("0");
		}
		else if (returnType.equals(Integer.TYPE)) {
			return Integer.valueOf('0');
		}
		else if (returnType.equals(Long.TYPE)) {
			return Long.valueOf('0');
		}
		else if (returnType.equals(Float.TYPE)) {
			return Float.valueOf('0');
		}
		else if (returnType.equals(Double.TYPE)) {
			return Double.valueOf('0');
		}
		else if (returnType.equals(Void.TYPE)) {
			return null;
		}
		throw new IllegalStateException("Primitive type with no default: " + returnType);
	}

}
