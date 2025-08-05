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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

@SpringJUnitConfig
class StepScopeDestructionCallbackIntegrationTests {

	@Autowired
	@Qualifier("proxied")
	private Step proxied;

	@Autowired
	@Qualifier("nested")
	private Step nested;

	@Autowired
	@Qualifier("ref")
	private Step ref;

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
	void testDisposableScopedProxy() throws Exception {
		assertNotNull(proxied);
		proxied.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	void testDisposableInnerScopedProxy() throws Exception {
		assertNotNull(nested);
		nested.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	void testProxiedScopedProxy() throws Exception {
		assertNotNull(nested);
		nested.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(4, TestAdvice.names.size());
		assertEquals("bar", TestAdvice.names.get(0));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	void testRefScopedProxy() throws Exception {
		assertNotNull(ref);
		ref.execute(new StepExecution("step", new JobExecution(0L), 1L));
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
