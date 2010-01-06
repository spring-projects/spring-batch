package org.springframework.batch.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
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
	public void testStepContext() throws Exception {
		TestContext testContext = getTestContext(new Object());
		listener.prepareTestInstance(testContext);
		listener.beforeTestMethod(testContext);
		StepContext context = StepSynchronizationManager.getContext();
		assertNotNull(context);
		listener.afterTestMethod(testContext);
		assertNull(StepSynchronizationManager.getContext());
	}

	@Test
	public void testWithStepExecution() throws Exception {
		testExecutionContext(new WithStepExecution());
	}

	@Test
	public void testWithMapForContext() throws Exception {
		testExecutionContext(new WithMap());
	}

	@Test
	public void testWithTwoMapForContext() throws Exception {
		testExecutionContext(new WithTwoMaps());
	}

	@Test
	public void testWithContextForContext() throws Exception {
		testExecutionContext(new WithExecutionContext());
	}

	@Test
	public void testWithTwpContextsForContext() throws Exception {
		testExecutionContext(new WithTwoMaps());
	}

	@Test
	public void testWithJobExecution() throws Exception {
		testJobParameters(new WithJobExecution());
	}

	@Test
	public void testWithMapForParameters() throws Exception {
		testJobParameters(new WithMap());
	}

	@Test
	public void testWithTwoMapsForParameters() throws Exception {
		testJobParameters(new WithParametersMap());
	}

	@Test
	public void testWithParameters() throws Exception {
		testJobParameters(new WithParameters());
	}

	@Test
	public void testWithTwoParameters() throws Exception {
		testJobParameters(new WithTwoParameters());
	}

	private void testExecutionContext(Object target) throws Exception {
		TestContext testContext = getTestContext(target);
		listener.prepareTestInstance(testContext);
		listener.beforeTestMethod(testContext);
		StepContext context = StepSynchronizationManager.getContext();
		assertNotNull(context);
		assertEquals("bar", context.getStepExecutionContext().get("foo"));
		listener.afterTestMethod(testContext);
		assertNull(StepSynchronizationManager.getContext());
	}

	private void testJobParameters(Object target) throws Exception {
		TestContext testContext = getTestContext(target);
		listener.prepareTestInstance(testContext);
		listener.beforeTestMethod(testContext);
		StepContext context = StepSynchronizationManager.getContext();
		assertNotNull(context);
		assertEquals("bar", context.getJobParameters().get("foo"));
		listener.afterTestMethod(testContext);
		assertNull(StepSynchronizationManager.getContext());
	}

	private static class WithStepExecution {
		private StepExecution execution = MetaDataInstanceFactory.createStepExecution();

		public WithStepExecution() {
			execution.getExecutionContext().putString("foo", "bar");
		}
	}

	@SuppressWarnings("unused")
	private static class WithJobExecution {
		private JobExecution execution = MetaDataInstanceFactory.createJobExecution("job", 11L, 123L, new JobParametersBuilder().addString("foo", "bar").toJobParameters());
	}

	@SuppressWarnings("unused")
	private static class WithMap {
		private Map<String, Object> context = Collections.singletonMap("foo", (Object) "bar");
	}

	@SuppressWarnings("unused")
	private static class WithTwoMaps {
		private Map<String, Object> executionContext = Collections.singletonMap("foo", (Object) "bar");

		private Map<String, Object> map = Collections.singletonMap("foo", (Object) "spam");
	}

	@SuppressWarnings("unused")
	private static class WithParametersMap {
		private Map<String, Object> jobParameters = Collections.singletonMap("foo", (Object) "bar");

		private Map<String, Object> map = Collections.singletonMap("foo", (Object) "spam");
	}

	@SuppressWarnings("unused")
	private static class WithExecutionContext {
		private ExecutionContext context = new ExecutionContext(Collections.singletonMap("foo", (Object) "bar"));
	}

	@SuppressWarnings("unused")
	public static class WithTwoExecutionContextw {
		private ExecutionContext executionContext = new ExecutionContext(Collections
				.singletonMap("foo", (Object) "bar"));

		private ExecutionContext context = new ExecutionContext(Collections.singletonMap("foo", (Object) "spam"));
	}

	@SuppressWarnings("unused")
	private static class WithParameters {
		private JobParameters params = new JobParametersBuilder().addString("foo", "bar").toJobParameters();
	}

	@SuppressWarnings("unused")
	private static class WithTwoParameters {
		private JobParameters jobParemeters = new JobParametersBuilder().addString("foo", "bar").toJobParameters();

		private JobParameters params = new JobParametersBuilder().addString("foo", "spam").toJobParameters();
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
