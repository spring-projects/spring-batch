/*
 * Copyright 2006-2021 the original author or authors.
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
package org.springframework.batch.infrastructure.item.adapter;

import java.lang.reflect.Method;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link MethodInvoker} that is a bit relaxed about its arguments. You can give it
 * arguments in the wrong order, or you can give it too many arguments, and it will try
 * and find a method that matches a subset.
 *
 * @author Dave Syer
 * @since 2.1
 */
@NullUnmarked // FIXME
public class HippyMethodInvoker extends MethodInvoker {

	@Override
	protected @Nullable Method findMatchingMethod() {
		String targetMethod = getTargetMethod();

		@Nullable Object[] arguments = getArguments();

		Class<?> targetClass = getTargetClass();
		Assert.state(targetClass != null, "No target class set");
		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Method matchingMethod = null;

		@Nullable Object[] transformedArguments = null;

		for (Method candidate : candidates) {
			if (candidate.getName().equals(targetMethod)) {
				Class<?>[] paramTypes = candidate.getParameterTypes();
				@Nullable Object[] candidateArguments = new Object[paramTypes.length];
				int assignedParameterCount = 0;
				for (Object argument : arguments) {
					for (int i = 0; i < paramTypes.length; i++) {
						// Pick the first assignable of the right type that
						// matches this slot and hasn't already been filled...
						if (ClassUtils.isAssignableValue(paramTypes[i], argument) && candidateArguments[i] == null) {
							candidateArguments[i] = argument;
							assignedParameterCount++;
							break;
						}
					}
				}
				if (paramTypes.length == assignedParameterCount) {
					int typeDiffWeight = getTypeDifferenceWeight(paramTypes, candidateArguments);
					if (typeDiffWeight < minTypeDiffWeight) {
						minTypeDiffWeight = typeDiffWeight;
						matchingMethod = candidate;
						transformedArguments = candidateArguments;
					}
				}
			}
		}

		if (transformedArguments == null) {
			throw new IllegalArgumentException("No matching arguments found for method: " + targetMethod);
		}

		setArguments(transformedArguments);
		return matchingMethod;

	}

}
