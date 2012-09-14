package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobScopeProxyTargetClassIntegrationTests implements BeanFactoryAware {

	@Autowired
	@Qualifier("simple")
	private TestCollaborator simple;

	private JobExecution jobExecution;

	private ListableBeanFactory beanFactory;

	private int beanCount;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Before
	public void start() {

		JobSynchronizationManager.close();
		jobExecution = new JobExecution(123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", "bar");

		jobExecution.setExecutionContext(executionContext);
		JobSynchronizationManager.register(jobExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@After
	public void cleanUp() {
		JobSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimpleProperty() throws Exception {
		assertEquals("bar", simple.getName());
		// Once the job context is set up it should be baked into the proxies
		// so changing it now should have no effect
		jobExecution.getExecutionContext().put("foo", "wrong!");
		assertEquals("bar", simple.getName());
	}

}
