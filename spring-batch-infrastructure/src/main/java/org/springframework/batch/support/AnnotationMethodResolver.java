/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.aop.support.AopUtils;

import org.jspecify.annotations.Nullable;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;

/**
 * {@link MethodResolver} implementation that finds a <em>single</em> Method on the given
 * Class that contains the specified annotation type.
 *
 * @author Mark Fisher
 * @author Mahmoud Ben Hassine
 */
public class AnnotationMethodResolver implements MethodResolver {

	private final Class<? extends Annotation> annotationType;

	/**
	 * Create a {@link MethodResolver} for the specified Method-level annotation type.
	 * @param annotationType establish the annotation to be used.
	 */
	public AnnotationMethodResolver(Class<? extends Annotation> annotationType) {
		Assert.notNull(annotationType, "annotationType must not be null");
		Assert.isTrue(
				ObjectUtils.containsElement(annotationType.getAnnotation(Target.class).value(), ElementType.METHOD),
				"Annotation [" + annotationType + "] is not a Method-level annotation.");
		this.annotationType = annotationType;
	}

	/**
	 * Find a <em>single</em> Method on the Class of the given candidate object that
	 * contains the annotation type for which this resolver is searching.
	 * @param candidate the instance whose Class will be checked for the annotation
	 * @return a single matching Method instance or <code>null</code> if the candidate's
	 * Class contains no Methods with the specified annotation
	 * @throws IllegalArgumentException if more than one Method has the specified
	 * annotation
	 */
	@Override
	public @Nullable Method findMethod(Object candidate) {
		Assert.notNull(candidate, "candidate object must not be null");
		Class<?> targetClass = AopUtils.getTargetClass(candidate);
		if (targetClass == null) {
			targetClass = candidate.getClass();
		}
		return this.findMethod(targetClass);
	}

	/**
	 * Find a <em>single</em> Method on the given Class that contains the annotation type
	 * for which this resolver is searching.
	 * @param clazz the Class instance to check for the annotation
	 * @return a single matching Method instance or <code>null</code> if the Class
	 * contains no Methods with the specified annotation
	 * @throws IllegalArgumentException if more than one Method has the specified
	 * annotation
	 */
	@Override
	public @Nullable Method findMethod(Class<?> clazz) {
		Assert.notNull(clazz, "class must not be null");
		final AtomicReference<Method> annotatedMethod = new AtomicReference<>();
		ReflectionUtils.doWithMethods(clazz, method -> {
			Annotation annotation = AnnotationUtils.findAnnotation(method, annotationType);
			if (annotation != null) {
				Assert.isNull(annotatedMethod.get(), "found more than one method on target class [" + clazz
						+ "] with the annotation type [" + annotationType + "]");
				annotatedMethod.set(method);
			}
		});
		return annotatedMethod.get();
	}

}
