package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AsyncStepScopeIntegrationTests implements BeanFactoryAware {

	private Log logger = LogFactory.getLog(getClass());

	@Autowired
	@Qualifier("simple")
	private Collaborator simple;

	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private ListableBeanFactory beanFactory;

	private int beanCount;

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Before
	public void countBeans() {
		StepSynchronizationManager.release();
		beanCount = beanFactory.getBeanDefinitionCount();
	}

	@After
	public void cleanUp() {
		StepSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimpleProperty() throws Exception {
		StepExecution stepExecution = new StepExecution("step", new JobExecution(0L), 123L);
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		executionContext.put("foo", "bar");
		StepSynchronizationManager.register(stepExecution);
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testGetMultipleInMultipleThreads() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			final Long id = 123L + i;
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				public String call() throws Exception {
					StepExecution stepExecution = new StepExecution(value, new JobExecution(0L), id);
					ExecutionContext executionContext = stepExecution.getExecutionContext();
					executionContext.put("foo", value);
					StepContext context = StepSynchronizationManager.register(stepExecution);
					logger.debug("Registered: " + context.getStepExecutionContext());
					try {
						return simple.getName();
					}
					finally {
						StepSynchronizationManager.close();
					}
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		int i = 0;
		for (FutureTask<String> task : tasks) {
			assertEquals("foo" + i, task.get());
			i++;
		}

	}

	@Test
	public void testGetSameInMultipleThreads() throws Exception {

		List<FutureTask<String>> tasks = new ArrayList<FutureTask<String>>();
		final StepExecution stepExecution = new StepExecution("foo", new JobExecution(0L), 123L);
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		executionContext.put("foo", "foo");
		StepSynchronizationManager.register(stepExecution);
		assertEquals("foo", simple.getName());

		for (int i = 0; i < 12; i++) {
			final String value = "foo" + i;
			FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
				public String call() throws Exception {
					ExecutionContext executionContext = stepExecution.getExecutionContext();
					executionContext.put("foo", value);
					StepContext context = StepSynchronizationManager.register(stepExecution);
					logger.debug("Registered: " + context.getStepExecutionContext());
					try {
						return simple.getName();
					}
					finally {
						StepSynchronizationManager.close();
					}
				}
			});
			tasks.add(task);
			taskExecutor.execute(task);
		}

		int i = 0;
		for (FutureTask<String> task : tasks) {
			assertEquals("foo", task.get());
			i++;
		}

		// Don't close the outer scope until all tasks are finished. This should
		// always be the case if using an AbstractStep
		StepSynchronizationManager.close();

	}

}
