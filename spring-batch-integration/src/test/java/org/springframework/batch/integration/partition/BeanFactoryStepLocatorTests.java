package org.springframework.batch.integration.partition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;


public class BeanFactoryStepLocatorTests {
	
	private BeanFactoryStepLocator stepLocator = new BeanFactoryStepLocator();
	private DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
	
	@Test
	public void testGetStep() throws Exception {
		beanFactory.registerSingleton("foo", new StubStep("foo"));
		stepLocator.setBeanFactory(beanFactory);
		assertNotNull(stepLocator.getStep("foo"));
	}

	@Test
	public void testGetStepNames() throws Exception {
		beanFactory.registerSingleton("foo", new StubStep("foo"));
		beanFactory.registerSingleton("bar", new StubStep("bar"));
		stepLocator.setBeanFactory(beanFactory);
		assertEquals(2, stepLocator.getStepNames().size());
	}

	private static final class StubStep implements Step {

		private String name;
		
		public StubStep(String name) {
			this.name = name;
		}

		public void execute(StepExecution stepExecution) throws JobInterruptedException {
		}

		public String getName() {
			return name;
		}

		public int getStartLimit() {
			return 0;
		}

		public boolean isAllowStartIfComplete() {
			return false;
		}
	}

}
