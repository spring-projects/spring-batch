/*
 * Copyright 2006-2010 the original author or authors.
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
package org.springframework.batch.test;

import java.lang.reflect.Method;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.JobContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.adapter.HippyMethodInvoker;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListener;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodCallback;

/**
 * A {@link TestExecutionListener} that sets up job-scope context for
 * dependency injection into unit tests. A {@link JobContext} will be created
 * for the duration of a test method and made available to any dependencies that
 * are injected. The default behaviour is just to create a {@link JobExecution} with fixed properties. Alternatively it
 * can be provided by the test case as a
 * factory methods returning the correct type. Example:
 * 
 * <pre>
 * &#064;ContextConfiguration
 * &#064;TestExecutionListeners({ DependencyInjectionTestExecutionListener.class, JobScopeTestExecutionListener.class })
 * &#064;RunWith(SpringJUnit4ClassRunner.class)
 * public class JobScopeTestExecutionListenerIntegrationTests {
 * 
 * 	// A job-scoped dependency configured in the ApplicationContext
 * 	&#064;Autowired
 * 	private ItemReader&lt;String&gt; reader;
 * 
 * 	public JobExecution getJobExecution() {
 * 		JobExecution execution = MetaDataInstanceFactory.createJobExecution();
 * 		execution.getExecutionContext().putString(&quot;foo&quot;, &quot;bar&quot;);
 * 		return execution;
 * 	}
 * 
 * 	&#064;Test
 * 	public void testJobScopedReader() {
 * 		// Job context is active here so the reader can be used,
 * 		// and the job execution context will contain foo=bar...
 * 		assertNotNull(reader.read());
 * 	}
 * 
 * }
 * </pre>
 * 
 * @author Dave Syer
 * @author Jimmy Praet
 */
public class JobScopeTestExecutionListener implements TestExecutionListener {

	private static final String JOB_EXECUTION = JobScopeTestExecutionListener.class.getName() + ".JOB_EXECUTION";

	/**
	 * Set up a {@link JobExecution} as a test context attribute.
	 * 
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#prepareTestInstance(TestContext)
	 */
	@Override
	public void prepareTestInstance(TestContext testContext) throws Exception {
		JobExecution jobExecution = getJobExecution(testContext);
		if (jobExecution != null) {
			testContext.setAttribute(JOB_EXECUTION, jobExecution);
		}
	}

	/**
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#beforeTestMethod(TestContext)
	 */
	@Override
	public void beforeTestMethod(org.springframework.test.context.TestContext testContext) throws Exception {
		if (testContext.hasAttribute(JOB_EXECUTION)) {
			JobExecution jobExecution = (JobExecution) testContext.getAttribute(JOB_EXECUTION);
			JobSynchronizationManager.register(jobExecution);
		}

	}

	/**
	 * @param testContext the current test context
	 * @throws Exception if there is a problem
	 * @see TestExecutionListener#afterTestMethod(TestContext)
	 */
	@Override
	public void afterTestMethod(TestContext testContext) throws Exception {
		if (testContext.hasAttribute(JOB_EXECUTION)) {
			JobSynchronizationManager.close();
		}
	}

	/*
	 * Support for Spring 3.0 (empty).
	 */
	@Override
	public void afterTestClass(TestContext testContext) throws Exception {
	}

	/*
	 * Support for Spring 3.0 (empty).
	 */
	@Override
	public void beforeTestClass(TestContext testContext) throws Exception {
	}
	
	/**
	 * Discover a {@link JobExecution} as a field in the test case or create
	 * one if none is available.
	 * 
	 * @param testContext the current test context
	 * @return a {@link JobExecution}
	 */
	protected JobExecution getJobExecution(TestContext testContext) {

		Object target = testContext.getTestInstance();

		ExtractorMethodCallback method = new ExtractorMethodCallback(JobExecution.class, "getJobExecution");
		ReflectionUtils.doWithMethods(target.getClass(), method);
		if (method.getName() != null) {
			HippyMethodInvoker invoker = new HippyMethodInvoker();
			invoker.setTargetObject(target);
			invoker.setTargetMethod(method.getName());
			try {
				invoker.prepare();
				return (JobExecution) invoker.invoke();
			}
			catch (Exception e) {
				throw new IllegalArgumentException("Could not create job execution from method: " + method.getName(),
						e);
			}
		}

		return MetaDataInstanceFactory.createJobExecution();
	}

	/**
	 * Look for a method returning the type provided, preferring one with the
	 * name provided.
	 */
	private final class ExtractorMethodCallback implements MethodCallback {
		private String preferredName;

		private final Class<?> preferredType;

		private Method result;

		public ExtractorMethodCallback(Class<?> preferredType, String preferredName) {
			super();
			this.preferredType = preferredType;
			this.preferredName = preferredName;
		}

		public String getName() {
			return result == null ? null : result.getName();
		}

		@Override
		public void doWith(Method method) throws IllegalArgumentException, IllegalAccessException {
			Class<?> type = method.getReturnType();
			if (preferredType.isAssignableFrom(type)) {
				if (result == null || method.getName().equals(preferredName)) {
					result = method;
				}
			}
		}
	}

}
