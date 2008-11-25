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
package org.springframework.batch.core.configuration.util;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

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
	 * @param paramsRequired boolean indicating whether the parameters are required, if false, a no args version of the
	 * method will be searched for.
	 * @param paramTypes - parameter types of the method to search for.
	 * @return MethodInvoker if the method is found, null if it is not.
	 */
	public static MethodInvoker createMethodInvokerByName(Object object, String methodName, boolean paramsRequired, Class<?>... paramTypes){
		Assert.notNull(object, "Object to invoke must not be null");
		Method method = ClassUtils.getMethodIfAvailable(object.getClass(), methodName, paramTypes);
		//if no method was found for the given parameters, and the parameters aren't required
		if(method == null && !paramsRequired){
			//try with no params
			method = ClassUtils.getMethodIfAvailable(object.getClass(), methodName, new Class[]{});
		}
		if(method == null){
			return null;
		}
		else{
			return new SimpleMethodInvoker(object, method);
		}
	}
	
	/**
	 * Create a {@link MethodInvoker} using the provided interface, and string methodname from that interface. 
	 * 
	 * @param object to be invoked
	 * @param methodName of the method to be invoked
	 * @param paramTypes - parameter types of the method to search for.
	 * @return MethodInvoker if the method is found, null if it is not.
	 */
	public static MethodInvoker getMethodInvokerForInterface(Class<?> iFace, String methodName, 
			Object object, Class<?>... paramTypes){
		
		if(iFace.isAssignableFrom(object.getClass())){
			return MethodInvokerUtils.createMethodInvokerByName(object, methodName, true, paramTypes);
		}
		else{
			return null;
		}
	}
	
	/**
	 * Create  {@link MethodInvoker} for the method with the provided annotation on the provided object.  It
	 * should be noted that annotations that cannot be applied to methods (i.e. that aren't annotated with an
	 * element type of METHOD) will cause an exception to be thrown.
	 * 
	 * @param annotationType to be searched for
	 * @param candidate to be invoked
	 * @return MethodInvoker for the provided annotation, null if none is found.
	 */
	public static MethodInvoker getMethodInvokerByAnnotation(final Class<? extends Annotation> annotationType, final Object candidate){
		Assert.notNull(candidate, "class must not be null");
		Assert.notNull(annotationType, "annotationType must not be null");
		Assert.isTrue(ObjectUtils.containsElement(
				annotationType.getAnnotation(Target.class).value(), ElementType.METHOD),
				"Annotation [" + annotationType + "] is not a Method-level annotation.");
		final AtomicReference<Method> annotatedMethod = new AtomicReference<Method>();
		ReflectionUtils.doWithMethods(candidate.getClass(), new ReflectionUtils.MethodCallback() {
			public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
				Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
				if (annotation != null) {
					Assert.isNull(annotatedMethod.get(), "found more than one method on target class ["
							+ candidate + "] with the annotation type [" + candidate + "]");
					annotatedMethod.set(method);
				}
			}
		});
		Method method = annotatedMethod.get();
		if(method == null){
			return null;
		}
		else{
			return new SimpleMethodInvoker(candidate, annotatedMethod.get());
		}
	}
}
