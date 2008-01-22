/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.execution.bootstrap.support;

import junit.framework.TestCase;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.ClassUtils;

/**
 * @author Dave Syer
 * 
 */
public class SimpleCommandLineJobRunnerTests extends TestCase {

	private static final String JOB_CONFIGURATION_PATH_KEY = SimpleCommandLineJobRunner.JOB_CONFIGURATION_PATH_KEY;
	private static final String JOB_NAME_KEY = SimpleCommandLineJobRunner.JOB_NAME_KEY;
	private static final String BATCH_EXECUTION_ENVIRONMENT_KEY = SimpleCommandLineJobRunner.BATCH_EXECUTION_ENVIRONMENT_KEY;
	private static final String BEAN_REF_CONTEXT_KEY = SimpleCommandLineJobRunner.BEAN_REF_CONTEXT_KEY;

	private static final String TEST_BATCH_ENVIRONMENT_KEY = "testBatchEnvironment";
	private static final String TEST_BATCH_ENVIRONMENT_NO_LAUNCHER_KEY = "testBatchEnvironmentNoLauncher";
	private static final String TEST_JOB_CONFIGURATION_WITH_ENVIRONMENT = ClassUtils.addResourcePathToPackagePath(SimpleCommandLineJobRunnerTests.class, "test-batch-environment-with-job.xml");

	BeanFactoryLocator beanFactoryLocator = ContextSingletonBeanFactoryLocator
			.getInstance();

	StubJobLauncher jobLauncher;
	StubSystemExiter systemExiter;

	protected void setUp() throws Exception {
		super.setUp();
		System.setProperty(BATCH_EXECUTION_ENVIRONMENT_KEY,
				TEST_BATCH_ENVIRONMENT_KEY);
		System.setProperty(BEAN_REF_CONTEXT_KEY,
				"beanRefContext.xml");
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		System.clearProperty(JOB_CONFIGURATION_PATH_KEY);
		System.clearProperty(JOB_NAME_KEY);
		System.clearProperty(BATCH_EXECUTION_ENVIRONMENT_KEY);
		System.clearProperty(BEAN_REF_CONTEXT_KEY);
	}

	public void testParentWithNoLauncher() {
		buildContext(TEST_BATCH_ENVIRONMENT_NO_LAUNCHER_KEY);
		assertNotNull(systemExiter);

		System.setProperty(BATCH_EXECUTION_ENVIRONMENT_KEY,
				TEST_BATCH_ENVIRONMENT_NO_LAUNCHER_KEY);

		SimpleCommandLineJobRunner.main(new String[0]);

		assertEquals(ExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR, systemExiter
				.getStatus());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.SimpleCommandLineJobRunner#main(java.lang.String[])}.
	 * 
	 * @throws Exception
	 */
	public void testNoBeanRefContext() throws Exception {
		// No error, so we assume it has worked!
		System.clearProperty(BATCH_EXECUTION_ENVIRONMENT_KEY);
		System.clearProperty(BEAN_REF_CONTEXT_KEY);
		System.setProperty(JOB_CONFIGURATION_PATH_KEY, TEST_JOB_CONFIGURATION_WITH_ENVIRONMENT);
		SimpleCommandLineJobRunner.main(new String[0]);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.SimpleCommandLineJobRunner#main(java.lang.String[])}.
	 * 
	 * @throws Exception
	 */
	public void testBeanRefContextButNoEnvironment() throws Exception {
		// No error, so we assume it has worked!
		System.clearProperty(BEAN_REF_CONTEXT_KEY);
		System.setProperty(JOB_CONFIGURATION_PATH_KEY, TEST_JOB_CONFIGURATION_WITH_ENVIRONMENT);
		try {
			SimpleCommandLineJobRunner.main(new String[0]);
		} catch (IllegalStateException e) {
			// expected
			String message = e.getMessage();
			assertTrue("Message should contain invalid property names:"+message, message.indexOf(SimpleCommandLineJobRunner.BATCH_EXECUTION_ENVIRONMENT_KEY)>=0);
			assertTrue("Message should contain invalid property names:"+message, message.indexOf(SimpleCommandLineJobRunner.BEAN_REF_CONTEXT_KEY)>=0);
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.SimpleCommandLineJobRunner#main(java.lang.String[])}.
	 * 
	 * @throws Exception
	 */
	public void testDefaultNameAndPath() throws Exception {

		buildContext(TEST_BATCH_ENVIRONMENT_KEY);
		assertNotNull(jobLauncher);
		assertNotNull(systemExiter);

		setReturnValue(ExitStatus.FINISHED);

		SimpleCommandLineJobRunner.main(new String[0]);

		assertEquals(ExitCodeMapper.JVM_EXITCODE_COMPLETED, systemExiter
				.getStatus());
		assertEquals(jobLauncher.getLastRunCalled(),
				StubJobLauncher.RUN_JOB_IDENTIFIER);
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.SimpleCommandLineJobRunner#main(java.lang.String[])}.
	 * 
	 * @throws Exception
	 */
	public void testCustomJobName() {

//		buildContext(TEST_BATCH_ENVIRONMENT_KEY);
//		assertNotNull(jobLauncher);
//		assertNotNull(systemExiter);
//		setReturnValue(ExitStatus.FINISHED);
//
//		System.setProperty(JOB_NAME_KEY, "foo");
//		SimpleCommandLineJobRunner.main(new String[0]);
//
//		assertEquals(ExitCodeMapper.JVM_EXITCODE_COMPLETED, systemExiter
//				.getStatus());
//		assertEquals(jobLauncher.getLastRunCalled(),
//				StubJobLauncher.RUN_JOB_IDENTIFIER);
	}

	private void setReturnValue(ExitStatus status) {
		JobExecution execution = new JobExecution(new JobInstance(new Long(1), new JobInstanceProperties(), new Job("foo")));
		
		execution.setExitStatus(status);
		jobLauncher.setReturnValue(execution);
	}
	
	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.bootstrap.support.SimpleCommandLineJobRunner#main(java.lang.String[])}.
	 * 
	 * @throws Exception
	 */
	public void testMainWithDefaultArguments() throws Exception {
		// We can only test this without running the whole test in another jvm
		// by using a special SystemExiter in the default configuration because
		// otherwise it calls System.exit() by default.
		SimpleCommandLineJobRunner.main(new String[0]);
	}

	public void testInvalidJobConfig() {
		// To test this without kicking off in a new jvm, we have to autowire
		// the launcher (in BatchCommandLineLauncher.start) from the parent,
		// *then* the child context.
		buildContext("batchExecutionEnvironment");
		assertNotNull(systemExiter);
		System.setProperty(JOB_CONFIGURATION_PATH_KEY, "foo");
		SimpleCommandLineJobRunner.main(new String[0]);
	}

	private void buildContext(String key) {
		ConfigurableApplicationContext context = (ClassPathXmlApplicationContext) beanFactoryLocator
				.useBeanFactory(key).getFactory();
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
	}

	public static class StubSystemExiter implements SystemExiter {

		private int status;

		public void exit(int status) {
			this.status = status;
		}

		public int getStatus() {
			return status;
		}
	}

	/**
	 * Public setter for the {@link StubJobLauncher} property.
	 * 
	 * @param jobLauncher
	 *            the jobLauncher to set
	 */
	public void setJobLauncher(StubJobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	/**
	 * Public setter for the {@link StubSystemExiter} property.
	 * 
	 * @param systemExiter
	 *            the systemExiter to set
	 */
	public void setSystemExiter(StubSystemExiter systemExiter) {
		this.systemExiter = systemExiter;
	}

}
