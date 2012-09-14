package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobScopeIntegrationTests {

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

	@Before
	@After
	public void start() {
		JobSynchronizationManager.close();
		TestJob.reset();
	}

	@Test
	public void testScopeCreation() throws Exception {
		vanilla.execute(new JobExecution(11L));
		assertNotNull(TestJob.getContext());
		assertNull(JobSynchronizationManager.getContext());
	}

	@Test
	public void testScopedProxy() throws Exception {
		proxied.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
		assertTrue("Scoped proxy not created", ((String) TestJob.getContext().getAttribute("collaborator.class"))
				.startsWith("class $Proxy"));
	}

	@Test
	public void testNestedScopedProxy() throws Exception {
		nested.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("foo", collaborator);
		String parent = (String) TestJob.getContext().getAttribute("parent");
		assertNotNull(parent);
		assertEquals("bar", parent);
		assertTrue("Scoped proxy not created", ((String) TestJob.getContext().getAttribute("parent.class"))
				.startsWith("class $Proxy"));
	}

	@Test
	public void testExecutionContext() throws Exception {
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
	public void testScopedProxyForReference() throws Exception {
		enhanced.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

	@Test
	public void testScopedProxyForSecondReference() throws Exception {
		doubleEnhanced.execute(new JobExecution(11L));
		assertTrue(TestJob.getContext().attributeNames().length > 0);
		String collaborator = (String) TestJob.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("bar", collaborator);
	}

}
