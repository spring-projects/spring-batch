/*
 * Copyright 2014-2024 the original author or authors.
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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.annotation.AnnotationUtils;

/**
 * Provides reflection based utilities for Spring Batch that are not available in Spring
 * Framework.
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 2.2.6
 */
public abstract class ReflectionUtils {

	private ReflectionUtils() {
	}

	/**
	 * Returns a {@link java.util.Set} of {@link java.lang.reflect.Method} instances that
	 * are annotated with the annotation provided.
	 * @param clazz The class to search for a method with the given annotation type
	 * @param annotationType The type of annotation to look for
	 * @return a set of {@link java.lang.reflect.Method} instances if any are found, an
	 * empty set if not.
	 */
	public static Set<Method> findMethod(Class<?> clazz, Class<? extends Annotation> annotationType) {
		return Arrays.stream(org.springframework.util.ReflectionUtils.getAllDeclaredMethods(clazz))
			.filter(method -> AnnotationUtils.findAnnotation(method, annotationType) != null)
			.collect(Collectors.toSet());
	}

}
