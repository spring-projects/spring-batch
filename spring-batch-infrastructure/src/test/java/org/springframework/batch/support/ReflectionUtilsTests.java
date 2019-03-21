/*
 * Copyright 2014 the original author or authors.
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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michael Minella
 * @since 2.2.6
 */
public class ReflectionUtilsTests {

	@Test
	public void testFindAnnotatedMethod() {
		Set<Method> methods = ReflectionUtils.findMethod(AnnotatedClass.class, Transactional.class);
		assertEquals(1, methods.size());
		assertEquals("toString", methods.iterator().next().getName());
	}

	@Test
	public void testFindNoAnnotatedMethod() {
		Set<Method> methods = ReflectionUtils.findMethod(AnnotatedClass.class, Autowired.class);
		assertEquals(0, methods.size());
	}

	@Test
	public void testFindAnnotatedMethodHierarchy() {
		Set<Method> methods = ReflectionUtils.findMethod(AnnotatedSubClass.class, Transactional.class);
		assertEquals(2, methods.size());

		boolean toStringFound = false;
		boolean methodOneFound = false;

		Iterator<Method> iterator = methods.iterator();

		String name = iterator.next().getName();

		if(name.equals("toString")) {
			toStringFound = true;
		} else if(name.equals("methodOne")) {
			methodOneFound = true;
		}

		name = iterator.next().getName();

		if(name.equals("toString")) {
			toStringFound = true;
		} else if(name.equals("methodOne")) {
			methodOneFound = true;
		}

		assertTrue(toStringFound && methodOneFound);
	}

	public static class AnnotatedClass {

		public void methodOne() {
			System.err.println("This is method 1");
		}

		@Transactional
		public String toString() {
			return "AnnotatedClass";
		}
	}

	public static class AnnotatedSubClass extends AnnotatedClass {

		@Transactional
		public void methodOne() {
			System.err.println("This is method 1 in the sub class");
		}
	}
}
