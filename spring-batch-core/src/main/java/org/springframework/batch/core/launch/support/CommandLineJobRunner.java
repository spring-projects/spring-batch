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
package org.springframework.batch.core.launch.support;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.runtime.JobParametersFactory;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

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
 * location. It can also load the job as well All dependencies of the launcher
 * will then be satisfied by autowiring by type from the combined application
 * context. Default values are provided for all fields except the
 * {@link JobLauncher} and {@link JobLocator}. Therefore, if autowiring fails
 * to set it (it should be noted that dependency checking is disabled because
 * most of the fields have default values and thus don't require dependencies to
 * be fulfilled via autowiring) then an exception will be thrown. It should also
 * be noted that even if an exception is thrown by this class, it will be mapped
 * to an integer and returned.
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
 * The arguments to this class are roughly as follows:
 * </p>
 * 
 * <code>
 * java jobPath jobName jobLauncherPath jobParameters...
 * </code>
 * 
 * <p>
 * <ul>
 * <li>jobPath: the xml application context containing a {@link Job}
 * <li>jobName: the bean id of the job.
 * <li>jobLauncherPath: the xml application context containing a
 * {@link JobLauncher}
 * <li>jobParameters: 0 to many parameters that will be used to launch a job.
 * </ul>
 * </p>
 * 
 * <p>
 * The combined application context must only contain one instance of a
 * {@link JobLauncher}. The job parameters passed in to the command line will
 * be converted to {@link Properties} by assuming that each individual element
 * is one parameter that is separated by an equals sign. For example,
 * "vendor.id=290232". Below is an example arguments list: "
 * 
 * <p>
 * <code>
 * java org.springframework.batch.execution.bootstrap.support.CommandLineJobRunner testJob.xml 
 * testJob schedule.date=2008/01/24 vendor.id=3902483920 
 * <code></p>
 * 
 * <p>Once arguments have been successfully parsed, autowiring will be used to set 
 * various dependencies.  The {@JobLauncher} for example, will be loaded this way.  If
 * none is contained in the bean factory (it searches by type) then a 
 * {@link BeanDefinitionStoreException} will be thrown.  The same exception will also
 * be thrown if there is more than one present.  Assuming the JobLauncher has been
 * set correctly, the jobName argument will be used to obtain an actual {@link Job}.
 * If a {@link JobLocator} has been set, then it will be used, if not the beanFactory
 * will be asked, using the jobName as the bean id.</p>
 * 
 * @author Dave Syer
 * @author Lucas Ward
 * @since 1.0
 */
public class CommandLineJobRunner {

	protected static final Log logger = LogFactory
			.getLog(CommandLineJobRunner.class);

	private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

	private JobLauncher launcher;

	private JobLocator jobLocator;

	private SystemExiter systemExiter = new JvmSystemExiter();

	private JobParametersFactory jobParametersFactory = new DefaultJobParametersFactory();

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

	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	/*
	 * Start a job by obtaining a combined classpath using the job launcher and
	 * job paths. If a JobLocator has been set, then use it to obtain an actual
	 * job, if not ask the context for it.
	 */
	int start(String jobPath, String jobName, String[] parameters) {

		try {
			ApplicationContext context = new ClassPathXmlApplicationContext(jobPath);
			context.getAutowireCapableBeanFactory().autowireBeanProperties(
					this, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

			Job job;
			if (jobLocator != null) {
				job = jobLocator.getJob(jobName);
			} else {
				job = (Job) context.getBean(jobName);
			}

			JobParameters jobParameters = jobParametersFactory
					.getJobParameters(StringUtils
							.splitArrayElementsIntoProperties(parameters, "="));

			JobExecution jobExecution = launcher.run(job, jobParameters);
			return exitCodeMapper.getExitCode(jobExecution.getExitStatus()
					.getExitCode());
		} catch (Throwable e) {
			logger.error("Job Terminated in error:", e);
			return exitCodeMapper.getExitCode(ExitStatus.FAILED.getExitCode());
		}
	}

	/**
	 * Launch a batch job using a {@link CommandLineJobRunner}. Creates a new
	 * Spring context for the job execution, and uses a common parent for all
	 * such contexts. No exception are thrown from this method, rather
	 * exceptions are logged and an integer returned through the exit status in
	 * a {@link JvmSystemExiter} (which can be overridden by defining one in the
	 * Spring context).
	 * 
	 * @param args
	 *            <p>
	 *            <ul>
	 *            <li>jobPath: the xml application context containing a
	 *            {@link Job}
	 *            <li>jobName: the bean id of the job.
	 *            <li>jobLauncherPath: the xml application context containing a
	 *            {@link JobLauncher}
	 *            <li>jobParameters: 0 to many parameters that will be used to
	 *            launch a job.
	 *            </ul>
	 *            </p>
	 */
	public static void main(String[] args) {

		CommandLineJobRunner command = new CommandLineJobRunner();

		if (args.length < 2) {
			logger
					.error("At least 2 arguments are required: JobPath and JobName.");
			command.exit(1);
		}

		String jobPath = args[0];
		String jobName = args[1];
		String[] parameters = new String[args.length - 2];
		System.arraycopy(args, 2, parameters, 0, args.length - 2);

		int result = command.start(jobPath, jobName, parameters);
		command.exit(result);
	}

}
