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
		assertEquals("destroyed", TestDisposableCollaborator.message);
	}

	@Test
	public void testDisposableInnerScopedProxy() throws Exception {
		assertNotNull(nested);
		nested.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals("destroyed", TestDisposableCollaborator.message);
	}

	@Test
	public void testProxiedScopedProxy() throws Exception {
		assertNotNull(nested);
		nested.execute(new StepExecution("step", new JobExecution(0L), 1L));
		assertEquals(2, TestAdvice.names.size());
		assertEquals("bar", TestAdvice.names.get(0));
		assertEquals("destroyed", TestDisposableCollaborator.message);
	}

	@Test
	public void testProxiedNormalBean() throws Exception {
		assertNotNull(nested);
		String name = foo.getName();
		assertEquals(1, TestAdvice.names.size());
		assertEquals(name, TestAdvice.names.get(0));
	}

}
