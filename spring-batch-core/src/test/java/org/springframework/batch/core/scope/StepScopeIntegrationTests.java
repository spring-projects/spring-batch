/*
 * Copyright 2008-2022 the original author or authors.
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class StepScopeIntegrationTests {

	private static final String PROXY_TO_STRING_REGEX = "class .*\\$Proxy\\d+";

	@Autowired
	@Qualifier("vanilla")
	private Step vanilla;

	@Autowired
	@Qualifier("proxied")
	private Step proxied;

	@Autowired
	@Qualifier("nested")
	private Step nested;

	@Autowired
	@Qualifier("enhanced")
	private Step enhanced;

	@Autowired
	@Qualifier("double")
	private Step doubleEnhanced;

	@BeforeEach
	@AfterEach
	void start() {
		StepSynchronizationManager.close();
		TestStep.reset();
	}

	@Test
	void testScopeCreation() throws Exception {
		vanilla.execute(
				new StepExecution(12L, "foo", new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters())));
		assertNotNull(TestStep.getContext());
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	void testScopedProxy() throws Exception {
		proxied.execute(
				new StepExecution(31L, "foo", new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters())));
		assertTrue(TestStep.getContext().attributeNames().length > 0);
		String collaborator = (String) TestStep.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
		assertTrue(((String) TestStep.getContext().getAttribute("collaborator.class")).matches(PROXY_TO_STRING_REGEX),
				"Scoped proxy not created");
	}

	@Test
	void testNestedScopedProxy() throws Exception {
		nested.execute(
				new StepExecution(31L, "foo", new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters())));
		assertTrue(TestStep.getContext().attributeNames().length > 0);
		String collaborator = (String) TestStep.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("foo", collaborator);
		String parent = (String) TestStep.getContext().getAttribute("parent");
		assertNotNull(parent);
		assertEquals("bar", parent);
		assertTrue(((String) TestStep.getContext().getAttribute("parent.class")).matches(PROXY_TO_STRING_REGEX),
				"Scoped proxy not created");
	}

	@Test
	void testExecutionContext() throws Exception {
		StepExecution stepExecution = new StepExecution(1L, "foo",
				new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters()));
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("name", "spam");
		stepExecution.setExecutionContext(executionContext);
		proxied.execute(stepExecution);
		assertTrue(TestStep.getContext().attributeNames().length > 0);
		String collaborator = (String) TestStep.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

	@Test
	void testScopedProxyForReference() throws Exception {
		enhanced.execute(
				new StepExecution(123L, "foo", new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters())));
		assertTrue(TestStep.getContext().attributeNames().length > 0);
		String collaborator = (String) TestStep.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

	@Test
	void testScopedProxyForSecondReference() throws Exception {
		doubleEnhanced.execute(
				new StepExecution(321L, "foo", new JobExecution(11L, new JobInstance(1L, "job"), new JobParameters())));
		assertTrue(TestStep.getContext().attributeNames().length > 0);
		String collaborator = (String) TestStep.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

}
