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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.execution.bootstrap.support.JvmSystemExiter;
import org.springframework.batch.execution.bootstrap.support.SimpleJvmExitCodeMapper;
import org.springframework.batch.execution.step.simple.SimpleExitCodeExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * @author Lucas Ward
 * @since 2.1
 */
public class BatchCommandLineLauncher {

	protected static final Log logger = LogFactory
			.getLog(BatchCommandLineLauncher.class);

	/**
	 * The key for the parent context.
	 */
	private static final String DEFAULT_PARENT_KEY = "batchExecutionEnvironment";
	private static final String DEFAULT_JOB_CONFIGURATION_PATH = "job-configuration.xml";

	private static final String JOB_CONFIGURATIN_PATH_KEY = "job.configuration.path";
	private static final String JOB_NAME_KEY = "job.name";
	private static final String BATCH_EXECUTION_ENVIRONMENT_KEY = "batch.execution.environment.key";

	private BeanFactoryLocator beanFactoryLocator;

	private JvmExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

	private ExitCodeExceptionClassifier exceptionClassifier = new SimpleExitCodeExceptionClassifier();

	private JobLauncher launcher;

	private SystemExiter systemExiter = new JvmSystemExiter();

	public BatchCommandLineLauncher() {
		beanFactoryLocator = ContextSingletonBeanFactoryLocator.getInstance();
	}

	/**
	 * Injection setter for the {@link JobLauncher}.
	 * 
	 * @param launcher
	 *            the launcher to set
	 */
	public void setLauncher(JobLauncher launcher) {
		this.launcher = launcher;
	}

	/**
	 * Injection setter for the {@link ExitCodeExceptionClassifier}
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(
			ExitCodeExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Injection setter for the {@link JvmExitCodeMapper}.
	 * 
	 * @param exitCodeMapper
	 *            the exitCodeMapper to set
	 */
	public void setExitCodeMapper(JvmExitCodeMapper exitCodeMapper) {
		this.exitCodeMapper = exitCodeMapper;
	}

	/**
	 * Injection setter for the {@link SystemExiter}.
	 * 
	 * @param systemExitor
	 */
	public void setSystemExiter(SystemExiter systemExitor) {
		this.systemExiter = systemExitor;
	}

	public SystemExiter getSystemExiter() {
		return systemExiter;
	}

	/**
	 * @param path
	 *            the path to a Spring context configuration for this job
	 * @param jobName
	 *            the name of the job execution to use
	 * @throws NoSuchJobConfigurationException
	 * @throws IllegalStateException
	 *             if JobLauncher is not autowired by the ApplicationContext
	 */
	int start(String path, String jobName, String parentKey) {

		ExitStatus status = ExitStatus.FAILED;
		ClassPathXmlApplicationContext context = null;
		
		try {
			ClassPathXmlApplicationContext parent = (ClassPathXmlApplicationContext) beanFactoryLocator
					.useBeanFactory(parentKey).getFactory();

			if (!path.endsWith(".xml")) {
				path = path + ".xml";
			}

			context = new ClassPathXmlApplicationContext(
					new String[] { path }, parent);

			context.getAutowireCapableBeanFactory().autowireBeanProperties(
					this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

			Assert.state(launcher != null,
						"JobLauncher must be provided in the parent ApplicationContext"
						+ ", check the context created within classpath*:beanRefContext.xml to ensure a JobLauncher"
						+ " is declared");

			if (!launcher.isRunning()) {
				if (jobName == null) {
					status = launcher.run();
				} else {
					status = launcher.run(jobName);
				}
			}
		} catch (NoSuchJobConfigurationException e) {
			logger.fatal("Could not locate JobConfiguration \"" + jobName
					+ "\"", e);
			status = new ExitStatus(false,
					JvmExitCodeMapper.BATCH_EXITCODE_NO_SUCH_JOBCONFIGURATION);
		} catch (Throwable t) {
			logger.fatal(t);
			status = exceptionClassifier.classifyForExitCode(t);
		} finally {
			if(context != null){
				try {
					context.stop();
				} finally {
					context.close();
				}
			}
		}
		return exitCodeMapper.getExitCode(status.getExitCode());
	}

	/**
	 * Launch a batch job using a {@link BatchCommandLineLauncher}. Creates a
	 * new Spring context for the job execution, and uses a common parent for
	 * all such contexts.
	 * 
	 * @param args
	 *            <ol>
	 *            <li> classpath location of resource to load job configuration
	 *            context (default "job-configuration.xml");</li>
	 *            <li>runtime name of Job (default "job-execution-id").</li>
	 *            <li> parent context key for use in pulling the correct
	 *            beanFactory from the beanRefContext.xml (@see
	 *            ContextSingletonBeanFactoryLocator)</li>
	 *            </ol>
	 * @throws NoSuchJobConfigurationException
	 */
	public static void main(String[] args) {

		String path = System.getProperty(JOB_CONFIGURATIN_PATH_KEY,
				DEFAULT_JOB_CONFIGURATION_PATH);
		String name = System.getProperty(JOB_NAME_KEY);
		String parentKey = System.getProperty(BATCH_EXECUTION_ENVIRONMENT_KEY,
				DEFAULT_PARENT_KEY);

		BatchCommandLineLauncher command = new BatchCommandLineLauncher();
		int result = command.start(path, name, parentKey);
		command.getSystemExiter().exit(result);
	}

}
