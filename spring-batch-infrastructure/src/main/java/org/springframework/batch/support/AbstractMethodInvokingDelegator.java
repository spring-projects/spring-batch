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

package org.springframework.batch.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.batch.io.exception.DynamicMethodInvocationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;

/**
 * Superclass for delegating classes which dynamically call a 
 * custom method of injected object.
 * Provides convenient API for dynamic method invocation shielding
 * subclasses from low-level details and exception handling. 
 * 
 * @author Robert Kasanicky
 */
public class AbstractMethodInvokingDelegator implements InitializingBean {
	
	private Object targetObject;
	
	private String targetMethod;
	
	private Object[] arguments;

	/**
	 * Invoker the target method with no arguments.
	 * @return object returned by invoked method
	 * @throws DynamicMethodInvocationException if the {@link MethodInvoker} used throws exception
	 */
	protected Object invokeDelegateMethod() {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		invoker.setArguments(arguments);
		return doInvoke(invoker);
	}
	
	/**
	 * Invokes the target method with given argument.
	 * @param object argument for the target method
	 * @return object returned by target method
	 * @throws DynamicMethodInvocationException if the {@link MethodInvoker} used throws exception
	 */
	protected Object invokeDelegateMethodWithArgument(Object object) {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		invoker.setArguments(new Object[]{object});
		return doInvoke(invoker);
	}
	
	/**
	 * Invokes the target method with given arguments.
	 * @param args arguments for the invoked method
	 * @return object returned by invoked method
	 * @throws DynamicMethodInvocationException if the {@link MethodInvoker} used throws exception
	 */
	protected Object invokeDelegateMethodWithArguments(Object[] args) {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		invoker.setArguments(args);
		return doInvoke(invoker);
	}
	
	/**
	 * Create a new configured instance of {@link MethodInvoker}.
	 */
	private MethodInvoker createMethodInvoker(Object targetObject, String targetMethod) {
		MethodInvoker invoker = new MethodInvoker();
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
	private Object doInvoke(MethodInvoker invoker) {
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
			return invoker.invoke();
		}
		catch (InvocationTargetException e) {
			throw new DynamicMethodInvocationException(e);
		}
		catch (IllegalAccessException e) {
			throw new DynamicMethodInvocationException(e);
		}	
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(targetObject);
		Assert.hasLength(targetMethod);
		Assert.state(targetClassDeclaresTargetMethod(), 
				"target class must declare a method with name matching the target method");
	}
	
	/**
	 * @return true if target class declares a method matching target method name
	 * with given number of arguments of appropriate type.
	 */
	private boolean targetClassDeclaresTargetMethod() {
		MethodInvoker invoker = createMethodInvoker(targetObject, targetMethod);
		Method[] methods = invoker.getTargetClass().getDeclaredMethods();
		String targetMethodName = invoker.getTargetMethod();

		for (int i=0; i < methods.length; i++) {
			if (methods[i].getName().equals(targetMethodName)) {
				Class[] params = methods[i].getParameterTypes();
				if (arguments == null) {
					return true;
				} else if (arguments.length == params.length) {
					boolean argumentsMatchParameters = true;
					for (int j = 0; j < params.length; j++) {
						if (!(params[j].isAssignableFrom(arguments[j].getClass()))) {
							argumentsMatchParameters = false;
						}
					}
					if (argumentsMatchParameters) return true;
				}
			}
		}
		
		return false;
	}

	/**
	 * @param targetObject the delegate - bean id can be used to set this value in Spring configuration
	 */
	public void setTargetObject(Object targetObject) {
		this.targetObject = targetObject;
	}

	/**
	 * @param targetMethod name of the method to be invoked on {@link #targetObject}.
	 */
	public void setTargetMethod(String targetMethod) {
		this.targetMethod = targetMethod;
	}
	
	/**
	 * @param arguments arguments values for the {{@link #targetMethod}.
	 * These are not expected to change during the lifetime of the delegator 
	 * and will be used only when the subclass tries to invoke the target method
	 * without providing explicit argument values.
	 */
	public void setArguments(Object[] arguments) {
		this.arguments = arguments;
	}
}
