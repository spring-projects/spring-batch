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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.configuration.NoSuchJobConfigurationException;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.execution.bootstrap.JobLauncher;
import org.springframework.batch.execution.step.simple.SimpleExitCodeExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

/**
 * <p>
 * Basic Launcher for starting jobs from the command line. In general, it is
 * assumed that this launcher will primarily be used to start a job via a script
 * from an Enterprise Scheduler. Therefore, exit codes are mapped to integers so
 * that schedulers can use the returned values to determine the next course of
 * action. The returned values can also be useful to operations teams in
 * determining what should happen upon failure. For example, a returned code of
 * 5 might mean that some resource wasn't available and the job should be
 * restarted. However, a code of 10 might mean that something critical has
 * happened and the issue should be escalated.
 * </p>
 * 
 * <p>
 * With any launch of a batch job within Spring Batch, a minimum of two contexts
 * must be loaded. One is the context containing the JobConfiguration, the other
 * contains the 'Execution Environment'. That is, the JobExecutorFacade (which
 * contains all the executors, plus the repository), the JobIdentifierFactory,
 * and a normal JobLauncher. This command line launcher loads these application
 * contexts by first loading the execution environment context via a
 * {@link ContextSingletonBeanFactoryLocator}, which will search for the
 * default key from classpath*:beanRefContext.xml to return the context. This
 * will then be used as the parent to the JobConfiguration context. All required
 * dependencies of the launcher will then be satisfied by autowiring by type
 * from the combined application context. Default values are provided for all
 * fields except the JobLauncher. Therefore, if autowiring fails to set it (it
 * should be noted that dependency checking is disabled because most of the
 * fields have default values and thus don't require dependencies to be
 * fulfilled via autowiring) then an exception will be thrown. It should also be
 * noted that even if an exception is thrown by this class, it will be mapped to
 * an integer and returned.
 * </p>
 * 
 * <p>
 * One odd field might be noticed in the launcher, SystemExiter. This class is
 * used to exit from the main method, rather than calling System.exit directly.
 * This is because unit testing a class the calls System.exit() is impossible
 * without kicking off the test within a new Jvm, which it is possible to do,
 * however it is a complex solution, much more so than strategizing the exiter.
 * </p>
 * 
 * <p>
 * VM Arguments vs. Program arguments: Because all of the arguments to the main
 * method are optional, VM arguments are used:
 * 
 * <ul>
 * <li>-Djob.configuration.path: the classpath location of the JobConfiguration
 * to use
 * <li>-Djob.name: job name to be passed to the {@link JobLauncher}
 * <li>-Dbatch.execution.environment.key: the key in beanRefContext.xml used to
 * load the execution envrionement.
 * </ul>
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @since 2.1
 */
public class BatchCommandLineLauncher {

	protected static final Log logger = LogFactory
			.getLog(BatchCommandLineLauncher.class);

	/**
	 * The default key for the parent context.
	 */
	public static final String DEFAULT_PARENT_KEY = "batchExecutionEnvironment";
	/**
	 * The default path to the job configuration.
	 */
	public static final String DEFAULT_JOB_CONFIGURATION_PATH = "job-configuration.xml";

	private static final String JOB_CONFIGURATION_PATH_KEY = "job.configuration.path";
	private static final String JOB_NAME_KEY = "job.name";
	private static final String BATCH_EXECUTION_ENVIRONMENT_KEY = "batch.execution.environment.key";

	private BeanFactoryLocator beanFactoryLocator;

	private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

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
	public void setExitCodeMapper(ExitCodeMapper exitCodeMapper) {
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

	/**
	 * Delegate to the exiter to (possibly) exit the VM gracefully.
	 * 
	 * @param status
	 */
	public void exit(int status) {
		systemExiter.exit(status);
	}

	/**
	 * @param path
	 *            the path to a Spring context configuration for this job
	 * @param jobName
	 *            the name of the job execution to use
	 * @parm parentKey the key to be loaded by
	 *       ContextSingletonBeanFactoryLocator and used as the parent context.
	 * @throws NoSuchJobConfigurationException
	 * @throws IllegalStateException
	 *             if JobLauncher is not autowired by the ApplicationContext
	 */
	int start(String path, String jobName, String parentKey) {

		ExitStatus status = ExitStatus.FAILED;
		ClassPathXmlApplicationContext context = null;

		try {
			ConfigurableApplicationContext parent = (ConfigurableApplicationContext) beanFactoryLocator
					.useBeanFactory(parentKey).getFactory();

			parent.getAutowireCapableBeanFactory().autowireBeanProperties(this,
					AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

			if (!path.endsWith(".xml")) {
				path = path + ".xml";
			}

			context = new ClassPathXmlApplicationContext(new String[] { path },
					parent);

			context.getAutowireCapableBeanFactory().autowireBeanProperties(
					this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

			Assert
					.state(
							launcher != null,
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
					ExitCodeMapper.NO_SUCH_JOB_CONFIGURATION);
		} catch (Throwable t) {
			logger.fatal(t);
			status = exceptionClassifier.classifyForExitCode(t);
		} finally {
			if (context != null) {
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
	 * all such contexts. No exception are thrown from this method, rather
	 * exceptions are logged and an integer returned through the exit status in
	 * a {@link JvmSystemExiter} (which can be overridden by defining one in the
	 * Spring context).
	 * 
	 * @param args
	 *            <ul>
	 *            <li>-Djob.configuration.path: the classpath location of the
	 *            JobConfiguration to use
	 *            <li>-Djob.name: job name to be passed to the
	 *            {@link JobLauncher}
	 *            <li>-Dbatch.execution.environment.key: the key in
	 *            beanRefContext.xml used to load the execution envrionment.
	 *            </ul>
	 */
	public static void main(String[] args) {

		String path = System.getProperty(JOB_CONFIGURATION_PATH_KEY,
				DEFAULT_JOB_CONFIGURATION_PATH);
		String name = System.getProperty(JOB_NAME_KEY);
		String parentKey = System.getProperty(BATCH_EXECUTION_ENVIRONMENT_KEY,
				DEFAULT_PARENT_KEY);
		
		BatchCommandLineLauncher command = new BatchCommandLineLauncher();
		int result = command.start(path, name, parentKey);
		command.exit(result);
	}

}
