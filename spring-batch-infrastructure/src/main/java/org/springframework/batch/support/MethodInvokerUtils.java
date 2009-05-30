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
package org.springframework.batch.support;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.framework.Advised;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Utility methods for create MethodInvoker instances.
 * 
 * @author Lucas Ward
 * @since 2.0
 */
public class MethodInvokerUtils {

	/**
	 * Create a {@link MethodInvoker} using the provided method name to search.
	 * 
	 * @param object to be invoked
	 * @param methodName of the method to be invoked
	 * @param paramsRequired boolean indicating whether the parameters are
	 * required, if false, a no args version of the method will be searched for.
	 * @param paramTypes - parameter types of the method to search for.
	 * @return MethodInvoker if the method is found, null if it is not.
	 */
	public static MethodInvoker getMethodInvokerByName(Object object, String methodName, boolean paramsRequired,
			Class<?>... paramTypes) {
		Assert.notNull(object, "Object to invoke must not be null");
		Method method = ClassUtils.getMethodIfAvailable(object.getClass(), methodName, paramTypes);
		if (method == null) {
			String errorMsg = "no method found with name [" + methodName + "] on class ["
					+ object.getClass().getSimpleName() + "] compatable with the signature ["
					+ getParamTypesString(paramTypes) + "].";
			Assert.isTrue(!paramsRequired, errorMsg);
			// if no method was found for the given parameters, and the
			// parameters aren't required, then try with no params
			method = ClassUtils.getMethodIfAvailable(object.getClass(), methodName, new Class[] {});
			Assert.notNull(method, errorMsg);
		}
		return new SimpleMethodInvoker(object, method);
	}

	/**
	 * Create a String representation of the array of parameter types.
	 * 
	 * @param paramTypes
	 * @return String
	 */
	public static String getParamTypesString(Class<?>... paramTypes) {
		StringBuffer paramTypesList = new StringBuffer("(");
		for (int i = 0; i < paramTypes.length; i++) {
			paramTypesList.append(paramTypes[i].getSimpleName());
			if (i + 1 < paramTypes.length) {
				paramTypesList.append(", ");
			}
		}
		return paramTypesList.append(")").toString();
	}

	/**
	 * Create a {@link MethodInvoker} using the provided interface, and method
	 * name from that interface.
	 * 
	 * @param cls the interface to search for the method named
	 * @param methodName of the method to be invoked
	 * @param object to be invoked
	 * @param paramTypes - parameter types of the method to search for.
	 * @return MethodInvoker if the method is found, null if it is not.
	 */
	public static MethodInvoker getMethodInvokerForInterface(Class<?> cls, String methodName, Object object,
			Class<?>... paramTypes) {

		if (cls.isAssignableFrom(object.getClass())) {
			return MethodInvokerUtils.getMethodInvokerByName(object, methodName, true, paramTypes);
		}
		else {
			return null;
		}
	}

	/**
	 * Create a MethodInvoker from the delegate based on the annotationType.
	 * Ensure that the annotated method has a valid set of parameters.
	 * 
	 * @param annotationType the annotation to scan for
	 * @param target the target object
	 * @param expectedParamTypes the expected parameter types for the method
	 * @return a MethodInvoker
	 */
	public static MethodInvoker getMethodInvokerByAnnotation(final Class<? extends Annotation> annotationType,
			final Object target, final Class<?>... expectedParamTypes) {
		MethodInvoker mi = MethodInvokerUtils.getMethodInvokerByAnnotation(annotationType, target);
		final Class<?> targetClass = (target instanceof Advised) ? ((Advised) target).getTargetSource()
				.getTargetClass() : target.getClass();
		if (mi != null) {
			ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
				public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
					Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
					if (annotation != null) {
						Class<?>[] paramTypes = method.getParameterTypes();
						if (paramTypes.length > 0) {
							String errorMsg = "The method [" + method.getName() + "] on target class ["
									+ targetClass.getSimpleName() + "] is incompatable with the signature ["
									+ getParamTypesString(expectedParamTypes) + "] expected for the annotation ["
									+ annotationType.getSimpleName() + "].";

							Assert.isTrue(paramTypes.length == expectedParamTypes.length, errorMsg);
							for (int i = 0; i < paramTypes.length; i++) {
								Assert.isTrue(expectedParamTypes[i].isAssignableFrom(paramTypes[i]), errorMsg);
							}
						}
					}
				}
			});
		}
		return mi;
	}

	/**
	 * Create {@link MethodInvoker} for the method with the provided annotation
	 * on the provided object. Annotations that cannot be applied to methods
	 * (i.e. that aren't annotated with an element type of METHOD) will cause an
	 * exception to be thrown.
	 * 
	 * @param annotationType to be searched for
	 * @param target to be invoked
	 * @return MethodInvoker for the provided annotation, null if none is found.
	 */
	public static MethodInvoker getMethodInvokerByAnnotation(final Class<? extends Annotation> annotationType,
			final Object target) {
		Assert.notNull(target, "Target must not be null");
		Assert.notNull(annotationType, "AnnotationType must not be null");
		Assert.isTrue(ObjectUtils.containsElement(annotationType.getAnnotation(Target.class).value(),
				ElementType.METHOD), "Annotation [" + annotationType + "] is not a Method-level annotation.");
		final Class<?> targetClass = (target instanceof Advised) ? ((Advised) target).getTargetSource()
				.getTargetClass() : target.getClass();
		if (targetClass == null) {
			// Proxy with no target cannot have annotations
			return null;
		}
		final AtomicReference<Method> annotatedMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(targetClass, new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
				if (annotation != null) {
					Assert.isNull(annotatedMethod.get(), "found more than one method on target class ["
							+ targetClass.getSimpleName() + "] with the annotation type ["
							+ annotationType.getSimpleName() + "].");
					annotatedMethod.set(method);
				}
			}
		});
		Method method = annotatedMethod.get();
		if (method == null) {
			return null;
		}
		else {
			return new SimpleMethodInvoker(target, annotatedMethod.get());
		}
	}

	/**
	 * Create a {@link MethodInvoker} for the delegate from a single public
	 * method.
	 * 
	 * @param target an object to search for an appropriate method
	 * @return a MethodInvoker that calls a method on the delegate
	 */
	public static <C, T> MethodInvoker getMethodInvokerForSingleArgument(Object target) {
		final AtomicReference<Method> methodHolder = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(target.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				if (method.getParameterTypes() == null || method.getParameterTypes().length != 1) {
					return;
				}
				if (method.getReturnType().equals(Void.TYPE) || ReflectionUtils.isEqualsMethod(method)) {
					return;
				}
				Assert.state(methodHolder.get() == null,
						"More than one non-void public method detected with single argument.");
				methodHolder.set(method);
			}
		});
		Method method = methodHolder.get();
		return new SimpleMethodInvoker(target, method);
	}
}
