/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import org.junit.Test;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Map;

import static org.junit.Assert.assertTrue;

/**
 * @author Thomas Risberg
 */
public class AutoRegisteringStepScopeTests {

	@Test
	@SuppressWarnings("resource")
	public void testJobElement() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/AutoRegisteringStepScopeForJobElementTests-context.xml");
		Map<String, StepScope> beans = ctx.getBeansOfType(StepScope.class);
		assertTrue("StepScope not defined properly", beans.size() == 1);
	}

	@Test
	@SuppressWarnings("resource")
	public void testStepElement() throws Exception {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/AutoRegisteringStepScopeForStepElementTests-context.xml");
		Map<String, StepScope> beans = ctx.getBeansOfType(StepScope.class);
		assertTrue("StepScope not defined properly", beans.size() == 1);
	}

}
