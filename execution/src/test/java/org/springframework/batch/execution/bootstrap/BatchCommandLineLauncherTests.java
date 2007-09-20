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
package org.springframework.batch.execution.bootstrap;

import org.easymock.MockControl;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import junit.framework.TestCase;

/**
 * @author Dave Syer
 *
 */
public class BatchCommandLineLauncherTests extends TestCase {
	
	private static final String DEFAULT_PARENT_KEY = "batchExecutionEnvironment";
	private static final String DEFAULT_JOB_CONFIGURATION_PATH = "job-configuration.xml";
	
	private static final String JOB_CONFIGURATION_PATH_KEY = "job.configuration.path";
	private static final String JOB_NAME_KEY = "job.name";
	private static final String BATCH_EXECUTION_ENVIRONMENT_KEY = "batch.execution.environment.key";
	
	private static final String TEST_BATCH_ENVIRONMENT_KEY = "testBatchEnvironment";
	private static final String TEST_BATCH_ENVIRONMENT_NO_LAUNCHER_KEY = "testBatchEnvironmentNoLauncher";
	
	BeanFactoryLocator beanFactoryLocator = ContextSingletonBeanFactoryLocator.getInstance();
	
	ClassPathXmlApplicationContext context;
	
	MockJobLauncher mockJobLauncher;
	MockSystemExiter mockSystemExiter;
	
	protected void setUp() throws Exception {
		super.setUp();
		
		context = (ClassPathXmlApplicationContext)beanFactoryLocator.useBeanFactory(TEST_BATCH_ENVIRONMENT_KEY).getFactory();
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
		System.setProperty(BATCH_EXECUTION_ENVIRONMENT_KEY, TEST_BATCH_ENVIRONMENT_KEY);
	}
	
	protected void tearDown() throws Exception {
		super.tearDown();
		System.clearProperty(JOB_CONFIGURATION_PATH_KEY);
		System.clearProperty(JOB_NAME_KEY);
		System.clearProperty(BATCH_EXECUTION_ENVIRONMENT_KEY);
	}
	
	public void setMockJobLauncher(MockJobLauncher mockJobLauncher){
		this.mockJobLauncher = mockJobLauncher;
	}
	
	public void setMockSystemExiter(MockSystemExiter mockSystemExiter) {
		this.mockSystemExiter = mockSystemExiter;
	}

	public void testParentWithNoLauncher(){
		context = (ClassPathXmlApplicationContext)beanFactoryLocator.useBeanFactory(TEST_BATCH_ENVIRONMENT_NO_LAUNCHER_KEY).getFactory();
		context.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
		System.setProperty(BATCH_EXECUTION_ENVIRONMENT_KEY, TEST_BATCH_ENVIRONMENT_NO_LAUNCHER_KEY);
		
		BatchCommandLineLauncher.main(new String[0]);
		
		assertEquals(JvmExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR, mockSystemExiter.getStatus());
	}
	
	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher#main(java.lang.String[])}.
	 * @throws Exception 
	 */
	public void testDefaultNameAndPath() throws Exception {

		mockJobLauncher.setReturnValue(ExitStatus.FINISHED);
		
		BatchCommandLineLauncher.main(new String[0]);
			
		assertEquals(JvmExitCodeMapper.JVM_EXITCODE_COMPLETED, mockSystemExiter.getStatus());
		assertEquals(mockJobLauncher.getLastRunCalled(), MockJobLauncher.RUN_NO_ARGS);
	}
	
	public void testCustomJobName(){
		
		mockJobLauncher.setReturnValue(ExitStatus.FINISHED);
		
		System.setProperty(JOB_NAME_KEY, "foo");
		BatchCommandLineLauncher.main(new String[0]);
			
		assertEquals(JvmExitCodeMapper.JVM_EXITCODE_COMPLETED, mockSystemExiter.getStatus());
		assertEquals(mockJobLauncher.getLastRunCalled(), MockJobLauncher.RUN_JOB_NAME);
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.bootstrap.BatchCommandLineLauncher#main(java.lang.String[])}.
	 * @throws Exception 
	 */
	public void testMainWithDefaultArguments() throws Exception {
		//can't test this without running the whole test in another jvm.
		//BatchCommandLineLauncher.main(new String[0]);
	}
	
	
	
	public void testInvalidJobConfig(){
		//also not testable without kicking off in a new jvm, since autowiring happens
		//after the context is loaded.
/*		System.setProperty(JOB_CONFIGURATION_PATH_KEY, "foo");
		
		BatchCommandLineLauncher.main(new String[0]);*/
	}
}
