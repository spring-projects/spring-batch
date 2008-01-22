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
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.execution.launch.JobLauncher;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifierFactory;
import org.springframework.batch.execution.step.simple.SimpleExitCodeExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;

/**
 * <p>
 * Basic launcher for starting jobs from the command line. In general, it is
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
 * With any launch of a batch job within Spring Batch, a Spring context
 * containing the Job and the 'Execution Environment' has to be created. This
 * command line launcher can be used to load that context from a single
 * location. It can also be used to first load the execution environment context
 * via a {@link ContextSingletonBeanFactoryLocator}. This will then be used as
 * the parent to the Job context. All required dependencies of the launcher will
 * then be satisfied by autowiring by type from the combined application
 * context. Default values are provided for all fields except the
 * {@link JobLauncher}. Therefore, if autowiring fails to set it (it should be
 * noted that dependency checking is disabled because most of the fields have
 * default values and thus don't require dependencies to be fulfilled via
 * autowiring) then an exception will be thrown. It should also be noted that
 * even if an exception is thrown by this class, it will be mapped to an integer
 * and returned.
 * </p>
 * 
 * <p>
 * Notice a property is available to set the {@link SystemExiter}. This class
 * is used to exit from the main method, rather than calling System.exit()
 * directly. This is because unit testing a class the calls System.exit() is
 * impossible without kicking off the test within a new Jvm, which it is
 * possible to do, however it is a complex solution, much more so than
 * strategizing the exiter.
 * </p>
 * 
 * <p>
 * VM Arguments vs. Program arguments: Because all of the arguments to the main
 * method are optional, System properties (VM arguments) are used (@see
 * {@link #main(String[])}).
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @since 2.1
 */
public class SimpleCommandLineJobRunner {

	protected static final Log logger = LogFactory.getLog(SimpleCommandLineJobRunner.class);

	/**
	 * The default path to the job configuration.
	 */
	public static final String DEFAULT_JOB_CONFIGURATION_PATH = "job-configuration.xml";

	public static final String JOB_CONFIGURATION_PATH_KEY = "job.configuration.path";

	public static final String JOB_NAME_KEY = "job.name";

	public static final String BATCH_EXECUTION_ENVIRONMENT_KEY = "batch.execution.environment.key";

	public static final String BEAN_REF_CONTEXT_KEY = "bean.ref.context";

	private JobIdentifierFactory jobIdentifierFactory = new ScheduledJobIdentifierFactory();

	private BeanFactoryLocator beanFactoryLocator;

	private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

	private ExitCodeExceptionClassifier exceptionClassifier = new SimpleExitCodeExceptionClassifier();

	private JobLauncher launcher;

	private SystemExiter systemExiter = new JvmSystemExiter();

	private String defaultJobName;

	public SimpleCommandLineJobRunner(String beanRefContextPath) {
		if (beanRefContextPath == null) {
			return;
		}
		beanFactoryLocator = ContextSingletonBeanFactoryLocator.getInstance(beanRefContextPath);
	}

	/**
	 * Setter for the name of the {@link Job} that this launcher will run.
	 * 
	 * @param jobName the job name to set
	 */
	public void setDefaultJobName(String defaultJobName) {
		this.defaultJobName = defaultJobName;
	}

	/**
	 * Setter for {@link JobIdentifierFactory}.
	 * 
	 * @param jobIdentifierFactory the {@link JobIdentifierFactory} to set
	 */
	public void setJobIdentifierFactory(JobIdentifierFactory jobIdentifierFactory) {
		this.jobIdentifierFactory = jobIdentifierFactory;
	}

	/**
	 * Injection setter for the {@link JobLauncher}.
	 * 
	 * @param launcher the launcher to set
	 */
	public void setLauncher(JobLauncher launcher) {
		this.launcher = launcher;
	}

	/**
	 * Injection setter for the {@link ExitCodeExceptionClassifier}
	 * 
	 * @param exceptionClassifier
	 */
	public void setExceptionClassifier(ExitCodeExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}

	/**
	 * Injection setter for the {@link JvmExitCodeMapper}.
	 * 
	 * @param exitCodeMapper the exitCodeMapper to set
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
	 * @param path the path to a Spring context configuration for this job
	 * @param jobName the name of the job execution to use
	 * @parm parentKey the key to be loaded by
	 * ContextSingletonBeanFactoryLocator and used as the parent context.
	 * @throws NoSuchJobException
	 * @throws IllegalStateException if JobLauncher is not autowired by the
	 * ApplicationContext
	 */
	int start(String path, String jobName, String parentKey) {

		ExitStatus status = ExitStatus.FAILED;
		ClassPathXmlApplicationContext context = null;

		try {
			ConfigurableApplicationContext parent = null;

			if (beanFactoryLocator != null) {
				parent = (ConfigurableApplicationContext) beanFactoryLocator.useBeanFactory(parentKey).getFactory();

				parent.getAutowireCapableBeanFactory().autowireBeanProperties(this,
						AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
			}

			if (!path.endsWith(".xml")) {
				path = path + ".xml";
			}

			context = new ClassPathXmlApplicationContext(new String[] { path }, parent);

			context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
					AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

			Assert.state(launcher != null, "JobLauncher must be provided in the parent ApplicationContext"
					+ ", check the context created within classpath*:beanRefContext.xml to ensure a JobLauncher"
					+ " is declared");

			if (jobName == null) {
				String[] names = context.getBeanNamesForType(Job.class);
				if (names.length == 1) {
					Job job = (Job) context.getBean(names[0]);
					jobName = job.getName();
				}
			}

			if (jobName == null) {
				jobName = defaultJobName;
			}

			if (jobName == null) {
				throw new NoSuchJobException("Null job name cannot be located.");
			}
			JobIdentifier runtimeInformation = jobIdentifierFactory.getJobIdentifier(jobName);

			if (!launcher.isRunning()) {
				status = launcher.run(runtimeInformation).getExitStatus();
			}
		}
		catch (NoSuchJobException e) {
			logger.fatal("Could not locate JobConfiguration \"" + jobName + "\"", e);
			status = new ExitStatus(false, ExitCodeMapper.NO_SUCH_JOB);
		}
		catch (Throwable t) {
			logger.fatal(t);
			status = exceptionClassifier.classifyForExitCode(t);
		}
		finally {
			if (context != null) {
				try {
					context.stop();
				}
				finally {
					context.close();
				}
			}
		}
		return exitCodeMapper.getExitCode(status.getExitCode());
	}

	/**
	 * Launch a batch job using a {@link SimpleCommandLineJobRunner}.
	 * Creates a new Spring context for the job execution, and uses a common
	 * parent for all such contexts. No exception are thrown from this method,
	 * rather exceptions are logged and an integer returned through the exit
	 * status in a {@link JvmSystemExiter} (which can be overridden by defining
	 * one in the Spring context).
	 * 
	 * @param args
	 * <ul>
	 * <li>-Djob.configuration.path: the classpath location of the
	 * JobConfiguration to use
	 * <li>-Djob.name: job name to be passed to the {@link JobLauncher}
	 * <li>-Dbatch.execution.environment.key: the key in beanRefContext.xml
	 * used to load the execution environment which will be the parent context
	 * for the job execution (mandatory if -Dbean.ref.context is specified).
	 * <li>-Dbean.ref.context: the location for beanRefContext.xml (optional,
	 * default is to only use the context specified in the
	 * job.configuration.path) (@see {@link SingletonBeanFactoryLocator}).</li>
	 * </ul>
	 */
	public static void main(String[] args) {

		String path = System.getProperty(JOB_CONFIGURATION_PATH_KEY, DEFAULT_JOB_CONFIGURATION_PATH);
		String name = System.getProperty(JOB_NAME_KEY);
		String beanRefContextPath = System.getProperty(BEAN_REF_CONTEXT_KEY);
		String parentKey = System.getProperty(BATCH_EXECUTION_ENVIRONMENT_KEY);

		Assert.state(!(beanRefContextPath == null && parentKey != null), "If you specify the "
				+ BATCH_EXECUTION_ENVIRONMENT_KEY + " you must also specify a path for the " + BEAN_REF_CONTEXT_KEY);

		SimpleCommandLineJobRunner command = new SimpleCommandLineJobRunner(beanRefContextPath);
		int result = command.start(path, name, parentKey);
		command.exit(result);
	}

}
