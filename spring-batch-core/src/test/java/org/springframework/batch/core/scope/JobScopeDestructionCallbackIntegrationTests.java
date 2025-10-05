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
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInterruptedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

@SpringJUnitConfig
class JobScopeDestructionCallbackIntegrationTests {

	@Autowired
	@Qualifier("proxied")
	private Job proxied;

	@Autowired
	@Qualifier("nested")
	private Job nested;

	@Autowired
	@Qualifier("ref")
	private Job ref;

	@Autowired
	@Qualifier("foo")
	private Collaborator foo;

	@BeforeEach
	@AfterEach
	void resetMessage() {
		TestDisposableCollaborator.message = "none";
		TestAdvice.names.clear();
	}

	@Test
	void testDisposableScopedProxy() throws JobInterruptedException {
		assertNotNull(proxied);
		proxied.execute(new JobExecution(1L, mock(), mock()));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	void testDisposableInnerScopedProxy() throws JobInterruptedException {
		assertNotNull(nested);
		nested.execute(new JobExecution(1L, mock(), mock()));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	void testProxiedScopedProxy() throws JobInterruptedException {
		assertNotNull(nested);
		nested.execute(new JobExecution(1L, mock(), mock()));
		assertEquals(4, TestAdvice.names.size());
		assertEquals("bar", TestAdvice.names.get(0));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	void testRefScopedProxy() throws JobInterruptedException {
		assertNotNull(ref);
		ref.execute(new JobExecution(1L, mock(), mock()));
		assertEquals(4, TestAdvice.names.size());
		assertEquals("spam", TestAdvice.names.get(0));
		assertEquals(2, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "bar:destroyed"));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "spam:destroyed"));
	}

	@Test
	void testProxiedNormalBean() {
		assertNotNull(nested);
		String name = foo.getName();
		assertEquals(1, TestAdvice.names.size());
		assertEquals(name, TestAdvice.names.get(0));
	}

}
