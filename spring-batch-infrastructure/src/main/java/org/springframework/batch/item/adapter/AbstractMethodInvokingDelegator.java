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

package org.springframework.batch.item.adapter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;

/**
 * Superclass for delegating classes which dynamically call a custom method of
 * injected object. Provides convenient API for dynamic method invocation
 * shielding subclasses from low-level details and exception handling.
 * 
 * {@link Exception}s thrown by a successfully invoked delegate method are
 * re-thrown without wrapping. In case the delegate method throws a
 * {@link Throwable} that doesn't subclass {@link Exception} it will be wrapped
 * by {@link InvocationTargetThrowableWrapper}.
 * 
 * @author Robert Kasanicky
 */
public abstract class AbstractMethodInvokingDelegator<T> implements InitializingBean {

	private Object targetObject;

	private String targetMethod;

	private Object[] arguments;

	/**
	 * Invoker the target method with arguments set by
	 * {@link #setArguments(Object[])}.
	 * @return object returned by invoked method
	 * @throws DynamicMethodInvocationException if the {@link MethodInvoker}
	 * used throws exception
	 */
	protected T invokeDelegateMethod() throws Exception {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		invoker.setArguments(arguments);
		return doInvoke(invoker);
	}

	/**
	 * Invokes the target method with given argument.
	 * @param object argument for the target method
	 * @return object returned by target method
	 * @throws DynamicMethodInvocationException if the {@link MethodInvoker}
	 * used throws exception
	 */
	protected T invokeDelegateMethodWithArgument(Object object) throws Exception {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		invoker.setArguments(new Object[] { object });
		return doInvoke(invoker);
	}

	/**
	 * Invokes the target method with given arguments.
	 * @param args arguments for the invoked method
	 * @return object returned by invoked method
	 * @throws DynamicMethodInvocationException if the {@link MethodInvoker}
	 * used throws exception
	 */
	protected T invokeDelegateMethodWithArguments(Object[] args) throws Exception {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		invoker.setArguments(args);
		return doInvoke(invoker);
	}

	/**
	 * Create a new configured instance of {@link MethodInvoker}.
	 */
	private MethodInvoker createMethodInvoker(Object targetObject, String targetMethod) {
		HippyMethodInvoker invoker = new HippyMethodInvoker();
		invoker.setTargetObject(targetObject);
		invoker.setTargetMethod(targetMethod);
		invoker.setArguments(arguments);
		return invoker;
	}

	/**
	 * Prepare and invoke the invoker, rethrow checked exceptions as unchecked.
	 * @param invoker configured invoker
	 * @return return value of the invoked method
	 */
	@SuppressWarnings("unchecked")
	private T doInvoke(MethodInvoker invoker) throws Exception {
		try {
			invoker.prepare();
		}
		catch (ClassNotFoundException e) {
			throw new DynamicMethodInvocationException(e);
		}
		catch (NoSuchMethodException e) {
			throw new DynamicMethodInvocationException(e);
		}

		try {
			return (T) invoker.invoke();
		}
		catch (InvocationTargetException e) {
			if (e.getCause() instanceof Exception) {
				throw (Exception) e.getCause();
			}
			else {
				throw new InvocationTargetThrowableWrapper(e.getCause());
			}
		}
		catch (IllegalAccessException e) {
			throw new DynamicMethodInvocationException(e);
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(targetObject);
		Assert.hasLength(targetMethod);
		Assert.state(targetClassDeclaresTargetMethod(),
				"target class must declare a method with matching name and parameter types");
	}

	/**
	 * @return true if target class declares a method matching target method
	 * name with given number of arguments of appropriate type.
	 */
	private boolean targetClassDeclaresTargetMethod() {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);

		Method[] memberMethods = invoker.getTargetClass().getMethods();
		Method[] declaredMethods = invoker.getTargetClass().getDeclaredMethods();

		List<Method> allMethods = new ArrayList<Method>();
		allMethods.addAll(Arrays.asList(memberMethods));
		allMethods.addAll(Arrays.asList(declaredMethods));

		String targetMethodName = invoker.getTargetMethod();

		for (Method method : allMethods) {
			if (method.getName().equals(targetMethodName)) {
				Class<?>[] params = method.getParameterTypes();
				if (arguments == null) {
					// don't check signature, assume arguments will be supplied
					// correctly at runtime
					return true;
				}
				if (arguments.length == params.length) {
					boolean argumentsMatchParameters = true;
					for (int j = 0; j < params.length; j++) {
						if (arguments[j] == null) {
							continue;
						}
						if (!(params[j].isAssignableFrom(arguments[j].getClass()))) {
							argumentsMatchParameters = false;
						}
					}
					if (argumentsMatchParameters)
						return true;
				}
			}
		}

		return false;
	}

	/**
	 * @param targetObject the delegate - bean id can be used to set this value
	 * in Spring configuration
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	/**
	 * @param targetMethod name of the method to be invoked on
	 * {@link #setTargetObject(Object)}.
	 */
	public void setTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * @param arguments arguments values for the {
	 * {@link #setTargetMethod(String)}. These will be used only when the
	 * subclass tries to invoke the target method without providing explicit
	 * argument values.
	 * 
	 * If arguments are set to not-null value {@link #afterPropertiesSet()} will
	 * check the values are compatible with target method's signature. In case
	 * arguments are null (not set) method signature will not be checked and it
	 * is assumed correct values will be supplied at runtime.
	 */
	public void setArguments(Object[] arguments) {
		this.arguments = arguments == null ? null : Arrays.asList(arguments).toArray();
	}

	/**
	 * Used to wrap a {@link Throwable} (not an {@link Exception}) thrown by a
	 * reflectively-invoked delegate.
	 * 
	 * @author Robert Kasanicky
	 */
	public static class InvocationTargetThrowableWrapper extends RuntimeException {

		public InvocationTargetThrowableWrapper(Throwable cause) {
			super(cause);
		}

	}
}
