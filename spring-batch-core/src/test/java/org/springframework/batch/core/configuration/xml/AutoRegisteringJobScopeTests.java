/*
 * Copyright 2013-2022 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.scope.JobScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Thomas Risberg
 * @author Jimmy Praet
 */
class AutoRegisteringJobScopeTests {

	@Test
	void testJobElement() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/AutoRegisteringJobScopeForJobElementTests-context.xml");
		Map<String, JobScope> beans = ctx.getBeansOfType(JobScope.class);
		assertEquals(1, beans.size(), "JobScope not defined properly");
	}

	@Test
	void testStepElement() {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/xml/AutoRegisteringJobScopeForStepElementTests-context.xml");
		Map<String, JobScope> beans = ctx.getBeansOfType(JobScope.class);
		assertEquals(1, beans.size(), "JobScope not defined properly");
	}

}
