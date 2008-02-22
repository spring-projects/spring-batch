/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.batch.execution.configuration;

import junit.framework.TestCase;

import org.springframework.batch.execution.step.tasklet.TaskletStep;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BatchNamespaceHandlerTaskletStepTests extends TestCase {

	private static final String PACKAGE = "org/springframework/batch/execution/configuration/";

	public void testTaskletStepOk() {
		new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepOk.xml");
	}

	public void testTaskletStepMissingTasklet() {
		try {
			new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepMissingTasklet.xml");
			fail("Expected BeanDefinitionParsingException");
		} catch (BeanDefinitionParsingException e) {	}
	}
	
	public void testTaskletStepRerunAlways() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepRerunAlways.xml");
		TaskletStep step = (TaskletStep) ctx.getBean("process");
		assertEquals(Integer.MAX_VALUE, step.getStartLimit());
		assertTrue(step.isAllowStartIfComplete());
	}
	
	public void testTaskletStepRerunNever() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepRerunNever.xml");
		TaskletStep step = (TaskletStep) ctx.getBean("process");
		assertEquals(1, step.getStartLimit());
		assertFalse(step.isAllowStartIfComplete());
	}

	public void testTaskletStepRerunIncomplete() {
		ApplicationContext ctx = new ClassPathXmlApplicationContext(PACKAGE + "BatchNamespaceHandlerTaskletStepRerunIncomplete.xml");
		TaskletStep step = (TaskletStep) ctx.getBean("process");
		assertEquals(Integer.MAX_VALUE, step.getStartLimit());
		assertFalse(step.isAllowStartIfComplete());
	}
}
