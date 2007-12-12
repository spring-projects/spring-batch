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

package org.springframework.batch.execution.bootstrap.support;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A {@link MethodInterceptor} that can mask a mismatch between the target and
 * proxy interfaces by converting the returned value to the correct type.
 * 
 * @author Dave Syer
 * 
 */
public class TypeConverterMethodInterceptor implements MethodInterceptor {

	// Get the default PropertyEditorRegistry free.
	private TypeConverter typeConverter = new SimpleTypeConverter();

	private boolean convertException = false;

	/**
	 * Set a flag that will cause exceptions during method invocation to be
	 * caught and treated as a result. This can be useful if used over JConsole,
	 * where exception reporting is a little weak (and in addition the class of
	 * the exception might not be available remotely).
	 * 
	 * @param convertException
	 *            the flag to set (default false)
	 */
	public void setConvertException(boolean convertException) {
		this.convertException = convertException;
	}

	/**
	 * Public setter for the {@link TypeConverter} property. Defaults to a
	 * {@link SimpleTypeConverter}.
	 * 
	 * @param typeConverter
	 *            the typeConverter to set
	 */
	public void setTypeConverter(TypeConverter typeConverter) {
		this.typeConverter = typeConverter;
	}

	/**
	 * Invoke the method with the same name and arguments on the target, but
	 * possibly with a different return type. If the return type doesn't match
	 * attempt to convert it.
	 * 
	 * @return an object that satisfies the signature of the proxy method.
	 * 
	 * @throws TypeMismatchException
	 *             if the target method returns an object that cannot be
	 *             converted to the desired type.
	 * 
	 * @see org.aopalliance.intercept.MethodInterceptor#invoke(org.aopalliance.intercept.MethodInvocation)
	 */
	public Object invoke(MethodInvocation invocation) throws Throwable {

		// The method called on the proxy
		Method invoked = invocation.getMethod();

		// The corresponding method on the target if there is one...
		Method method = ReflectionUtils.findMethod(invocation.getThis()
				.getClass(), invoked.getName(), invoked.getParameterTypes());

		// If there was no such method do nothing... TODO: throw Exception?
		if (method == null) {
			return null;
		}

		// Invoke the target method
		Object result = null;

		try {
			result = ReflectionUtils.invokeMethod(method, invocation.getThis(),
					invocation.getArguments());
		} catch (Throwable e) {
			if (convertException) {
				result = e;
			} else {
				throw e;
			}
		}
		if (result == null) {
			return null;
		}

		// If the return type doesn't match, try and convert it
		if (!ClassUtils.isAssignableValue(invoked.getReturnType(), result)) {
			result = convert(result, invoked.getReturnType());
		}
		return result;

	}

	private Object convert(Object result, Class returnType) {
		// Provide some simple conversion algorithms natively for String results
		if (returnType.isAssignableFrom(String.class)) {
			if (result instanceof Throwable) {
				Throwable e = (Throwable) result;
				e.fillInStackTrace();
				StringWriter writer = new StringWriter();
				e.printStackTrace(new PrintWriter(writer));
				result = writer.toString();
			}
			return result.toString();
		}
		return typeConverter.convertIfNecessary(result, returnType);
	}

}
