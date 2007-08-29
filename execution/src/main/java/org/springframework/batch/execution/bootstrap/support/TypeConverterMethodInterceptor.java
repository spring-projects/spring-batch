package org.springframework.batch.execution.bootstrap.support;

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

	// Get the default PropertyEditorRegistry free. TODO: make this
	// configurable.
	private TypeConverter typeConverter = new SimpleTypeConverter();

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
		Object result = ReflectionUtils.invokeMethod(method, invocation
				.getThis(), invocation.getArguments());
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
		if (returnType.isAssignableFrom(String.class)) {
			return result.toString();
		}
		return typeConverter.convertIfNecessary(result, returnType);
	}

}
