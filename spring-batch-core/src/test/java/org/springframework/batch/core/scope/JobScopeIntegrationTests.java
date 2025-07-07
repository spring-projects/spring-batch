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
package org.springframework.batch.core.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class JobScopeIntegrationTests {

	private static final String PROXY_TO_STRING_REGEX = "class .*\\$Proxy\\d+";

	@Autowired
	@Qualifier("vanilla")
	private Job vanilla;

	@Autowired
	@Qualifier("proxied")
	private Job proxied;

	@Autowired
	@Qualifier("nested")
	private Job nested;

	@Autowired
	@Qualifier("enhanced")
	private Job enhanced;

	@Autowired
	@Qualifier("double")
	private Job doubleEnhanced;

	@BeforeEach
	@AfterEach
	void start() {
		JobSynchronizationManager.close();
		TestJob.reset();
	}

	@Test
	void testScopeCreation() {
		vanilla.execute(new JobExecution(11L));
		assertNotNull(TestJob.getContext());
		assertNull(JobSynchronizationManager.getContext());
	}

	@Test
	void testScopedProxy() {
		proxied.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
		assertTrue(((String) TestJob.getContext().getAttribute("collaborator.class")).matches(PROXY_TO_STRING_REGEX),
				"Scoped proxy not created");
	}

	@Test
	void testNestedScopedProxy() {
		nested.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("foo", collaborator);
		String parent = (String) TestJob.getContext().getAttribute("parent");
		assertNotNull(parent);
		assertEquals("bar", parent);
		assertTrue(((String) TestJob.getContext().getAttribute("parent.class")).matches(PROXY_TO_STRING_REGEX),
				"Scoped proxy not created");
	}

	@Test
	void testExecutionContext() {
		JobExecution stepExecution = new JobExecution(11L);
		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("name", "spam");
		stepExecution.setExecutionContext(executionContext);
		proxied.execute(stepExecution);
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

	@Test
	void testScopedProxyForReference() {
		enhanced.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

	@Test
	void testScopedProxyForSecondReference() {
		doubleEnhanced.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

}
