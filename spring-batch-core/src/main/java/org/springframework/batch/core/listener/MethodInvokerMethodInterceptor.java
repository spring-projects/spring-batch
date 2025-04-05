/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.listener;

import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.support.MethodInvoker;

/**
 * {@link MethodInterceptor} that, given a map of method names and {@link MethodInvoker}s,
 * will execute all methods tied to a particular method name, with the provided arguments.
 * The only possible return value that is handled is of type ExitStatus, since the only
 * StepListener implementation that isn't void is
 * {@link StepExecutionListener#afterStep(org.springframework.batch.core.StepExecution)} ,
 * which returns ExitStatus.
 *
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @since 2.0
 * @see MethodInvoker
 */
public class MethodInvokerMethodInterceptor implements MethodInterceptor {

	private final Map<String, Set<MethodInvoker>> invokerMap;

	private final boolean ordered;

	public MethodInvokerMethodInterceptor(Map<String, Set<MethodInvoker>> invokerMap) {
		this(invokerMap, false);
	}

	public MethodInvokerMethodInterceptor(Map<String, Set<MethodInvoker>> invokerMap, boolean ordered) {
		this.ordered = ordered;
		this.invokerMap = invokerMap;
	}

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {

		String methodName = invocation.getMethod().getName();
		if (ordered && methodName.equals("getOrder")) {
			return invocation.proceed();
		}

		Set<MethodInvoker> invokers = invokerMap.get(methodName);

		if (invokers == null) {
			return null;
		}
		ExitStatus status = null;
		for (MethodInvoker invoker : invokers) {
			Object retVal = invoker.invokeMethod(invocation.getArguments());
			if (retVal instanceof ExitStatus exitStatus) {
				if (status != null) {
					status = status.and(exitStatus);
				}
				else {
					status = exitStatus;
				}
			}
		}

		// The only possible return values are ExitStatus or int (from Ordered)
		return status;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MethodInvokerMethodInterceptor other)) {
			return false;
		}
		return invokerMap.equals(other.invokerMap);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return invokerMap.hashCode();
	}

}
