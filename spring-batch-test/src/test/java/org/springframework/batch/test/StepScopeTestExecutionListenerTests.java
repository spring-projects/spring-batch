package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;

/**
 * @author Dave Syer
 * @since 2.1
 */
@ContextConfiguration
public class StepScopeTestExecutionListenerTests {

	private StepScopeTestExecutionListener listener = new StepScopeTestExecutionListener();

	@Test
	public void testDefaultStepContext() throws Exception {
		TestContext testContext = getTestContext(new Object());
		listener.prepareTestInstance(testContext);
		listener.beforeTestMethod(testContext);
		StepContext context = StepSynchronizationManager.getContext();
		assertNotNull(context);
		listener.afterTestMethod(testContext);
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	public void testWithStepExecutionFactory() throws Exception {
		testExecutionContext(new WithStepExecutionFactory());
	}

	@Test
	public void testWithParameters() throws Exception {
		testJobParameters(new WithStepExecutionFactory());
	}

	private void testExecutionContext(Object target) throws Exception {
		TestContext testContext = getTestContext(target);
		listener.prepareTestInstance(testContext);
		try {
			listener.beforeTestMethod(testContext);
			StepContext context = StepSynchronizationManager.getContext();
			assertNotNull(context);
			assertEquals("bar", context.getStepExecutionContext().get("foo"));
		}
		finally {
			listener.afterTestMethod(testContext);
		}
		assertNull(StepSynchronizationManager.getContext());
	}

	private void testJobParameters(Object target) throws Exception {
		TestContext testContext = getTestContext(target);
		listener.prepareTestInstance(testContext);
		try {
			listener.beforeTestMethod(testContext);
			StepContext context = StepSynchronizationManager.getContext();
			assertNotNull(context);
			assertEquals("spam", context.getJobParameters().get("foo"));
		}
		finally {
			listener.afterTestMethod(testContext);
		}
		assertNull(StepSynchronizationManager.getContext());
	}

	@SuppressWarnings("unused")
	private static class WithStepExecutionFactory {
		public StepExecution getStepExecution() {
			JobExecution jobExecution = MetaDataInstanceFactory.createJobExecution("job", 11L, 123L,
					new JobParametersBuilder().addString("foo", "spam").toJobParameters());
			StepExecution stepExecution = jobExecution.createStepExecution("step");
			stepExecution.getExecutionContext().putString("foo", "bar");
			return stepExecution;
		}
	}

	private TestContext getTestContext(Object target) throws Exception {
		return new MockTestContextManager(target, getClass()).getContext();
	}

	private final class MockTestContextManager extends TestContextManager {

		private MockTestContextManager(Object target, Class<?> testClass) throws Exception {
			super(testClass);
			prepareTestInstance(target);
		}

		public TestContext getContext() {
			return getTestContext();
		}

	}

}
