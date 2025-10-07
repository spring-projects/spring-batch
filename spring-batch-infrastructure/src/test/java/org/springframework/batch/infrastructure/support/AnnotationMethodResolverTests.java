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

package org.springframework.batch.infrastructure.support;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.AnnotationMethodResolver;

/**
 * @author Mark Fisher
 * @author Mahmoud Ben Hassine
 */
class AnnotationMethodResolverTests {

	@Test
	void singleAnnotation() {
		AnnotationMethodResolver resolver = new AnnotationMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(SingleAnnotationTestBean.class);
		assertNotNull(method);
	}

	@Test
	void multipleAnnotations() {
		AnnotationMethodResolver resolver = new AnnotationMethodResolver(TestAnnotation.class);
		assertThrows(IllegalArgumentException.class, () -> resolver.findMethod(MultipleAnnotationTestBean.class));
	}

	@Test
	void noAnnotations() {
		AnnotationMethodResolver resolver = new AnnotationMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(NoAnnotationTestBean.class);
		assertNull(method);
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private @interface TestAnnotation {

	}

	@SuppressWarnings("unused")
	private static class SingleAnnotationTestBean {

		@TestAnnotation
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		public String lowerCase(String s) {
			return s.toLowerCase();
		}

	}

	private static class MultipleAnnotationTestBean {

		@TestAnnotation
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		@TestAnnotation
		public String lowerCase(String s) {
			return s.toLowerCase();
		}

	}

	@SuppressWarnings("unused")
	private static class NoAnnotationTestBean {

		public String upperCase(String s) {
			return s.toUpperCase();
		}

		String lowerCase(String s) {
			return s.toLowerCase();
		}

	}

}
