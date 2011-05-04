/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.item.adapter;

import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link MethodInvoker} that is a bit relaxed about its arguments. You can
 * give it arguments in the wrong order or you can give it too many arguments
 * and it will try and find a method that matches a subset.
 * 
 * @author Dave Syer
 * 
 * @since 2.1
 */
public class HippyMethodInvoker extends MethodInvoker {

	@Override
	protected Method findMatchingMethod() {
		String targetMethod = getTargetMethod();
		Object[] arguments = getArguments();
		int argCount = arguments.length;

		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(getTargetClass());
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Method matchingMethod = null;

		Object[] transformedArguments = null;
		int transformedArgumentCount = 0;

		for (int i = 0; i < candidates.length; i++) {
			Method candidate = candidates[i];
			if (candidate.getName().equals(targetMethod)) {
				Class<?>[] paramTypes = candidate.getParameterTypes();
				Object[] candidateArguments = new Object[paramTypes.length];
				int assignedParameterCount = 0;
				boolean assigned = paramTypes.length==0;
				for (int j = 0; j < arguments.length; j++) {
					for (int k = 0; k < paramTypes.length; k++) {
						// Pick the first assignable of the right type that
						// matches this slot and hasn't already been filled...
						if (ClassUtils.isAssignableValue(paramTypes[k], arguments[j]) && candidateArguments[k] == null) {
							candidateArguments[k] = arguments[j];
							assignedParameterCount++;
							assigned = true;
							break;
						}
					}
				}
				if (assigned && paramTypes.length <= argCount) {
					int typeDiffWeight = getTypeDifferenceWeight(paramTypes, candidateArguments);
					if (typeDiffWeight < minTypeDiffWeight) {
						minTypeDiffWeight = typeDiffWeight;
						matchingMethod = candidate;
						transformedArguments = candidateArguments;
						transformedArgumentCount = assignedParameterCount;
					}
				}
			}
		}

		if (transformedArguments == null) {
			throw new IllegalArgumentException("No matching arguments found for method: " + targetMethod);
		}

		if (transformedArgumentCount < transformedArguments.length) {
			throw new IllegalArgumentException("Only " + transformedArgumentCount + " out of "
					+ transformedArguments.length + " arguments could be assigned.");
		}

		setArguments(transformedArguments);
		return matchingMethod;

	}
}
