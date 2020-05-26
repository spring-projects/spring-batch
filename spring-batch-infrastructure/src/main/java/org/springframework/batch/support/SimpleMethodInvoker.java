/*
 * Copyright 2002-2019 the original author or authors.
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

/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.batch.support;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.aop.framework.Advised;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Simple implementation of the {@link MethodInvoker} interface that invokes a
 * method on an object. If the method has no arguments, but arguments are
 * provided, they are ignored and the method is invoked anyway. If there are
 * more arguments than there are provided, then an exception is thrown.
 * 
 * @author Lucas Ward
 * @since 2.0
 */
public class SimpleMethodInvoker implements MethodInvoker {

	private final Object object;

	private Method method;

	public SimpleMethodInvoker(Object object, Method method) {
		Assert.notNull(object, "Object to invoke must not be null");
		Assert.notNull(method, "Method to invoke must not be null");
		this.method = method;
		this.object = object;
	}

	public SimpleMethodInvoker(Object object, String methodName, Class<?>... paramTypes) {
		Assert.notNull(object, "Object to invoke must not be null");
		this.method = ClassUtils.getMethodIfAvailable(object.getClass(), methodName, paramTypes);
		if (this.method == null) {
			// try with no params
			this.method = ClassUtils.getMethodIfAvailable(object.getClass(), methodName);
		}
		if (this.method == null) {
			throw new IllegalArgumentException("No methods found for name: [" + methodName + "] in class: ["
					+ object.getClass() + "] with arguments of type: [" + Arrays.toString(paramTypes) + "]");
		}
		this.object = object;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.core.configuration.util.MethodInvoker#invokeMethod
	 * (java.lang.Object[])
	 */
    @Nullable
	@Override
	public Object invokeMethod(Object... args) {

		Class<?>[] parameterTypes = method.getParameterTypes();
		Object[] invokeArgs;
		if (parameterTypes.length == 0) {
			invokeArgs = new Object[] {};
		}
		else if (parameterTypes.length != args.length) {
			throw new IllegalArgumentException("Wrong number of arguments, expected no more than: ["
					+ parameterTypes.length + "]");
		}
		else {
			invokeArgs = args;
		}

		method.setAccessible(true);

		try {
			// Extract the target from an Advised as late as possible
			// in case it contains a lazy initialization
			Object target = extractTarget(object, method);
			return method.invoke(target, invokeArgs);
		}
		catch (Exception e) {
			throw new IllegalArgumentException("Unable to invoke method: [" + method + "] on object: [" + object
					+ "] with arguments: [" + Arrays.toString(args) + "]", e);
		}
	}

	private Object extractTarget(Object target, Method method) {
		if (target instanceof Advised) {
			Object source;
			try {
				source = ((Advised) target).getTargetSource().getTarget();
			}
			catch (Exception e) {
				throw new IllegalStateException("Could not extract target from proxy", e);
			}
			if (source instanceof Advised) {
				source = extractTarget(source, method);
			}
			if (method.getDeclaringClass().isAssignableFrom(source.getClass())) {
				target = source;
			}
		}
		return target;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof SimpleMethodInvoker)) {
			return false;
		}

		if (obj == this) {
			return true;
		}
		SimpleMethodInvoker rhs = (SimpleMethodInvoker) obj;
		return (rhs.method.equals(this.method)) && (rhs.object.equals(this.object));
	}

	@Override
	public int hashCode() {
		int result = 25;
		result = 31 * result + object.hashCode();
		result = 31 * result + method.hashCode();
		return result;
	}
}
