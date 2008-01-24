/*
 * Copyright 2006-2008 the original author or authors.
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
import org.springframework.batch.core.domain.IncorrectJobCountException;
import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.runtime.JobParametersFactory;
import org.springframework.batch.execution.launch.JobLauncher;
import org.springframework.batch.execution.step.simple.SimpleExitCodeExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.access.SingletonBeanFactoryLocator;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

/**
 * @author Lucas Ward
 * 
 */
public class CommandLineJobRunner {

	protected static final Log logger = LogFactory
			.getLog(CommandLineJobRunner.class);

	private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

	private ExitCodeExceptionClassifier exceptionClassifier = new SimpleExitCodeExceptionClassifier();

	private JobLauncher launcher;

	private SystemExiter systemExiter = new JvmSystemExiter();

	private JobParametersFactory jobParametersFactory = new ScheduledJobParametersFactory();

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

	int start(String jobPath, String environmentPath, String[] parameters) {

		try {
			ApplicationContext context = new ClassPathXmlApplicationContext(new String[] {
					jobPath, environmentPath });
			context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
					AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);
			Job job = getJob(context);

			JobParameters jobParameters = jobParametersFactory.getJobParameters(StringUtils
					.splitArrayElementsIntoProperties(parameters, "="));

			JobExecution jobExecution = launcher.run(job, jobParameters);
			return exitCodeMapper.getExitCode(jobExecution.getExitStatus()
					.getExitCode());
		} catch (Throwable e) {
			logger.error("Job Terminated in error:", e);
			return exitCodeMapper.getExitCode(exceptionClassifier
					.classifyForExitCode(e).getExitCode());
		}
	}

	private Job getJob(ApplicationContext context)
			throws IncorrectJobCountException, NoSuchJobException {

		String[] jobs = context.getBeanNamesForType(Job.class);

		if (jobs.length > 1) {
			logger.error("More than one job exists in the provided context: [" + "jobPath" + "]");
			throw new IncorrectJobCountException(
					"More than one job exists in the provided context. Bean Names: ["
							+ jobs + "]");
		} else if (jobs.length == 0) {
			throw new NoSuchJobException("No jobs found in the provided context.");
		}

		return (Job) context.getBean(jobs[0]);
	}

	/**
	 * Launch a batch job using a {@link SimpleCommandLineJobRunner}. Creates a
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
	 *            beanRefContext.xml used to load the execution environment
	 *            which will be the parent context for the job execution
	 *            (mandatory if -Dbean.ref.context is specified).
	 *            <li>-Dbean.ref.context: the location for beanRefContext.xml
	 *            (optional, default is to only use the context specified in the
	 *            job.configuration.path) (@see
	 *            {@link SingletonBeanFactoryLocator}).</li>
	 *            </ul>
	 */
	public static void main(String[] args) {

		String jobPath = args[0];
		String executionPath = args[1];
		String[] parameters = new String[args.length - 2];
		System.arraycopy(args, 2, parameters, 0, args.length - 2);

		CommandLineJobRunner command = new CommandLineJobRunner();
		int result = command.start(jobPath, executionPath, parameters);
		command.exit(result);
	}

}
