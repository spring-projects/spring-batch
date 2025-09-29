/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Dave Syer
 * @author Jimmy Praet
 * @author Mahmoud Ben Hassine
 */
class JobScopeTests {

	private final JobScope scope = new JobScope();

	private final JobExecution jobExecution = new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters());

	private JobContext context;

	@BeforeEach
	void setUp() {
		context = JobSynchronizationManager.register(jobExecution);
	}

	@AfterEach
	void tearDown() {
		JobSynchronizationManager.release();
	}

	@Test
	void testGetWithNoContext() {
		final String foo = "bar";
		JobSynchronizationManager.release();
		assertThrows(IllegalStateException.class, () -> scope.get("foo", (ObjectFactory<String>) () -> foo));
	}

	@Test
	void testGetWithNothingAlreadyThere() {
		final String foo = "bar";
		Object value = scope.get("foo", (ObjectFactory<String>) () -> foo);
		assertEquals(foo, value);
		assertTrue(context.hasAttribute("foo"));
	}

	@Test
	void testGetWithSomethingAlreadyThere() {
		context.setAttribute("foo", "bar");
		Object value = scope.get("foo", (ObjectFactory<String>) () -> null);
		assertEquals("bar", value);
		assertTrue(context.hasAttribute("foo"));
	}

	@Test
	void testGetConversationId() {
		String id = scope.getConversationId();
		assertNotNull(id);
	}

	@Test
	void testRegisterDestructionCallback() {
		final List<String> list = new ArrayList<>();
		context.setAttribute("foo", "bar");
		scope.registerDestructionCallback("foo", () -> list.add("foo"));
		assertEquals(0, list.size());
		// When the context is closed, provided the attribute exists the
		// callback is called...
		context.close();
		assertEquals(1, list.size());
	}

	@Test
	void testRegisterAnotherDestructionCallback() {
		final List<String> list = new ArrayList<>();
		context.setAttribute("foo", "bar");
		scope.registerDestructionCallback("foo", () -> list.add("foo"));
		scope.registerDestructionCallback("foo", () -> list.add("bar"));
		assertEquals(0, list.size());
		// When the context is closed, provided the attribute exists the
		// callback is called...
		context.close();
		assertEquals(2, list.size());
	}

	@Test
	void testRemove() {
		context.setAttribute("foo", "bar");
		scope.remove("foo");
		assertFalse(context.hasAttribute("foo"));
	}

	@Test
	void testOrder() {
		assertEquals(Integer.MAX_VALUE, scope.getOrder());
		scope.setOrder(11);
		assertEquals(11, scope.getOrder());
	}

	@Test
	void testName() {
		scope.setName("foo");
		StaticApplicationContext beanFactory = new StaticApplicationContext();
		scope.postProcessBeanFactory(beanFactory.getDefaultListableBeanFactory());
		String[] scopes = beanFactory.getDefaultListableBeanFactory().getRegisteredScopeNames();
		assertEquals(1, scopes.length);
		assertEquals("foo", scopes[0]);
	}

}
