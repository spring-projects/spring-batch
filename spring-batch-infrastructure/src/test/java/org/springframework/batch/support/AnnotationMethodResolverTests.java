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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Test;
import org.springframework.batch.support.AnnotationMethodResolver;

/**
 * @author Mark Fisher
 */
public class AnnotationMethodResolverTests {

	@Test
	public void singleAnnotation() {
		AnnotationMethodResolver resolver = new AnnotationMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(SingleAnnotationTestBean.class);
		assertNotNull(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotations() {
		AnnotationMethodResolver resolver = new AnnotationMethodResolver(TestAnnotation.class);
		resolver.findMethod(MultipleAnnotationTestBean.class);
	}

	@Test
	public void noAnnotations() {
		AnnotationMethodResolver resolver = new AnnotationMethodResolver(TestAnnotation.class);
		Method method = resolver.findMethod(NoAnnotationTestBean.class);
		assertNull(method);
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation {
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


	@SuppressWarnings("unused")
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
