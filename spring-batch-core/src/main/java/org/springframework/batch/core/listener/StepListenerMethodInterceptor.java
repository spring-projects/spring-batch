/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.batch.core.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.batch.core.configuration.util.MethodInvoker;

/**
 * @author Lucas Ward
 *
 */
public class StepListenerMethodInterceptor implements MethodInterceptor{

	private final Map<String, Set<MethodInvoker>> invokerMap;

	public StepListenerMethodInterceptor(Map<String, Set<MethodInvoker>> invokerMap) {
		this.invokerMap = invokerMap;
	}
	
	public Object invoke(MethodInvocation invocation) throws Throwable {
		
		String methodName = invocation.getMethod().getName();
		Set<MethodInvoker> invokers = invokerMap.get(methodName);
		
		if(invokers == null){
			return null;
		}
		
		for(MethodInvoker invoker : invokers){
			invoker.invokeMethod(invocation.getArguments());
		}
		
		return null;
	}
	
	
}
