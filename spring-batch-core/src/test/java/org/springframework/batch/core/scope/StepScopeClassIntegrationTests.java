package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
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
@Ignore // Maybe one day support class replacement?
public class StepScopeClassIntegrationTests implements BeanFactoryAware {


	@Autowired
	@Qualifier("value")
	private Collaborator value;

	@Autowired
	@Qualifier("nested")
	private Collaborator nested;

	private StepExecution stepExecution;

	private ListableBeanFactory beanFactory;

	private int beanCount;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Before
	public void start() {
		start("bar");
	}

	private void start(String foo) {

		StepSynchronizationManager.close();
		stepExecution = new StepExecution("foo", new JobExecution(11L), 123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", foo);
		executionContext.put("type", TestCollaborator.class.getName());

		stepExecution.setExecutionContext(executionContext);
		StepSynchronizationManager.register(stepExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@After
	public void stop() {
		StepSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimpleValue() throws Exception {
		assertEquals("foo", value.getName());
	}

	@Test
	public void testNested() throws Exception {
		assertEquals("bar", nested.getParent().getName());
	}

}
