/*
 * Copyright 2008-2010 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.StringUtils;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class StepScopeDestructionCallbackIntegrationTests {

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

	@Before
	@After
	public void resetMessage() throws Exception {
		TestDisposableCollaborator.message = "none";
		TestAdvice.names.clear();
	}

	@Test
	public void testDisposableScopedProxy() throws Exception {
		assertNotNull(proxied);
		proxied.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	public void testDisposableInnerScopedProxy() throws Exception {
		assertNotNull(nested);
		nested.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	public void testProxiedScopedProxy() throws Exception {
		assertNotNull(nested);
		nested.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(4, TestAdvice.names.size());
		assertEquals("bar", TestAdvice.names.get(0));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
	}

	@Test
	public void testRefScopedProxy() throws Exception {
		assertNotNull(ref);
		ref.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(4, TestAdvice.names.size());
		assertEquals("spam", TestAdvice.names.get(0));
		assertEquals(2, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "destroyed"));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "bar:destroyed"));
		assertEquals(1, StringUtils.countOccurrencesOf(TestDisposableCollaborator.message, "spam:destroyed"));
	}

	@Test
	public void testProxiedNormalBean() throws Exception {
		assertNotNull(nested);
		String name = foo.getName();
		assertEquals(1, TestAdvice.names.size());
		assertEquals(name, TestAdvice.names.get(0));
	}

}
