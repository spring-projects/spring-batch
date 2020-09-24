/*
 * Copyright 2006-2020 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotFailedException;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobExecutionNotStoppedException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.Assert;
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
 * containing the {@link Job} and some execution context has to be created. This
 * command line launcher can be used to load the job and its context from a
 * single location. All dependencies of the launcher will then be satisfied by
 * autowiring by type from the combined application context. Default values are
 * provided for all fields except the {@link JobLauncher} and {@link JobLocator}
 * . Therefore, if autowiring fails to set it (it should be noted that
 * dependency checking is disabled because most of the fields have default
 * values and thus don't require dependencies to be fulfilled via autowiring)
 * then an exception will be thrown. It should also be noted that even if an
 * exception is thrown by this class, it will be mapped to an integer and
 * returned.
 * </p>
 *
 * <p>
 * Notice a property is available to set the {@link SystemExiter}. This class is
 * used to exit from the main method, rather than calling System.exit()
 * directly. This is because unit testing a class the calls System.exit() is
 * impossible without kicking off the test within a new JVM, which it is
 * possible to do, however it is a complex solution, much more so than
 * strategizing the exiter.
 * </p>
 *
 * <p>
 * The arguments to this class can be provided on the command line (separated by
 * spaces), or through stdin (separated by new line). They are as follows:
 * </p>
 *
 * <code>
 * jobPath &lt;options&gt; jobIdentifier (jobParameters)*
 * </code>
 *
 * <p>
 * The command line options are as follows
 * </p>
 * <ul>
 * <li>jobPath: the xml application context containing a {@link Job}
 * <li>-restart: (optional) to restart the last failed execution</li>
 * <li>-stop: (optional) to stop a running execution</li>
 * <li>-abandon: (optional) to abandon a stopped execution</li>
 * <li>-next: (optional) to start the next in a sequence according to the
 * {@link JobParametersIncrementer} in the {@link Job}</li>
 * <li>jobIdentifier: the name of the job or the id of a job execution (for
 * -stop, -abandon or -restart).
 * <li>jobParameters: 0 to many parameters that will be used to launch a job
 * specified in the form of <code>key=value</code> pairs.
 * </ul>
 *
 * <p>
 * If the <code>-next</code> option is used the parameters on the command line
 * (if any) are appended to those retrieved from the incrementer, overriding any
 * with the same key.
 * </p>
 *
 * <p>
 * The combined application context must contain only one instance of
 * {@link JobLauncher}. The job parameters passed in to the command line will be
 * converted to {@link Properties} by assuming that each individual element is
 * one parameter that is separated by an equals sign. For example,
 * "vendor.id=290232". The resulting properties instance is converted to
 * {@link JobParameters} using a {@link JobParametersConverter} from the
 * application context (if there is one, or a
 * {@link DefaultJobParametersConverter} otherwise). Below is an example
 * arguments list: "</p>
 *
 * <p>
 * <code>
 * java org.springframework.batch.core.launch.support.CommandLineJobRunner testJob.xml
 * testJob schedule.date=2008/01/24 vendor.id=3902483920
 * </code>
 * </p>
 *
 * <p>
 * By default, the `CommandLineJobRunner` uses a {@link DefaultJobParametersConverter}
 * which implicitly converts key/value pairs to identifying job parameters.
 * However, it is possible to explicitly specify which job parameters are identifying
 * and which are not by prefixing them with `+` or `-` respectively. In the following
 * example, `schedule.date` is an identifying job parameter while `vendor.id` is not:
 * </p>
 * 
 * <p>
 * <code>
 * java org.springframework.batch.core.launch.support.CommandLineJobRunner testJob.xml
 * testJob +schedule.date=2008/01/24 -vendor.id=3902483920
 * </code>
 * </p>
 *
 * <p>This behaviour can be overridden by using a custom `JobParametersConverter`.</p>
 *
 * <p>
 * Once arguments have been successfully parsed, autowiring will be used to set
 * various dependencies. The {@link JobLauncher} for example, will be
 * loaded this way. If none is contained in the bean factory (it searches by
 * type) then a {@link BeanDefinitionStoreException} will be thrown. The same
 * exception will also be thrown if there is more than one present. Assuming the
 * JobLauncher has been set correctly, the jobIdentifier argument will be used
 * to obtain an actual {@link Job}. If a {@link JobLocator} has been set, then
 * it will be used, if not the beanFactory will be asked, using the
 * jobIdentifier as the bean id.
 * </p>
 *
 * @author Dave Syer
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 * @since 1.0
 */
public class CommandLineJobRunner {

	protected static final Log logger = LogFactory.getLog(CommandLineJobRunner.class);

	private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

	private JobLauncher launcher;

	private JobLocator jobLocator;

	// Package private for unit test
	private static SystemExiter systemExiter = new JvmSystemExiter();

	private static String message = "";

	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	private JobExplorer jobExplorer;

	private JobRepository jobRepository;

	private final static List<String> VALID_OPTS = Arrays.asList(new String [] {"-restart", "-next", "-stop", "-abandon"});

	/**
	 * Injection setter for the {@link JobLauncher}.
	 *
	 * @param launcher the launcher to set
	 */
	public void setLauncher(JobLauncher launcher) {
		this.launcher = launcher;
	}

	/**
	 * @param jobRepository the jobRepository to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Injection setter for {@link JobExplorer}.
	 *
	 * @param jobExplorer the {@link JobExplorer} to set
	 */
	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	/**
	 * Injection setter for the {@link ExitCodeMapper}.
	 *
	 * @param exitCodeMapper the exitCodeMapper to set
	 */
	public void setExitCodeMapper(ExitCodeMapper exitCodeMapper) {
		this.exitCodeMapper = exitCodeMapper;
	}

	/**
	 * Static setter for the {@link SystemExiter} so it can be adjusted before
	 * dependency injection. Typically overridden by
	 * {@link #setSystemExiter(SystemExiter)}.
	 *
	 * @param systemExiter {@link SystemExiter} instance to be used by CommandLineJobRunner instance.
	 */
	public static void presetSystemExiter(SystemExiter systemExiter) {
		CommandLineJobRunner.systemExiter = systemExiter;
	}

	/**
	 * Retrieve the error message set by an instance of
	 * {@link CommandLineJobRunner} as it exits. Empty if the last job launched
	 * was successful.
	 *
	 * @return the error message
	 */
	public static String getErrorMessage() {
		return message;
	}

	/**
	 * Injection setter for the {@link SystemExiter}.
	 *
	 * @param systemExiter {@link SystemExiter} instance to be used by CommandLineJobRunner instance.
	 */
	public void setSystemExiter(SystemExiter systemExiter) {
		CommandLineJobRunner.systemExiter = systemExiter;
	}

	/**
	 * Injection setter for {@link JobParametersConverter}.
	 *
	 * @param jobParametersConverter instance of {@link JobParametersConverter}
	 * to be used by the CommandLineJobRunner instance.
	 */
	public void setJobParametersConverter(JobParametersConverter jobParametersConverter) {
		this.jobParametersConverter = jobParametersConverter;
	}

	/**
	 * Delegate to the exiter to (possibly) exit the VM gracefully.
	 *
	 * @param status int exit code that should be reported.
	 */
	public void exit(int status) {
		systemExiter.exit(status);
	}

	/**
	 * {@link JobLocator} to find a job to run.
	 * @param jobLocator a {@link JobLocator}
	 */
	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}

	/*
	 * Start a job by obtaining a combined classpath using the job launcher and
	 * job paths. If a JobLocator has been set, then use it to obtain an actual
	 * job, if not ask the context for it.
	 */
	@SuppressWarnings("resource")
	int start(String jobPath, String jobIdentifier, String[] parameters, Set<String> opts) {

		ConfigurableApplicationContext context = null;

		try {
			try {
				context = new AnnotationConfigApplicationContext(Class.forName(jobPath));
			} catch (ClassNotFoundException cnfe) {
				context = new ClassPathXmlApplicationContext(jobPath);
			}

			context.getAutowireCapableBeanFactory().autowireBeanProperties(this,
					AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

			Assert.state(launcher != null, "A JobLauncher must be provided.  Please add one to the configuration.");
			if (opts.contains("-restart") || opts.contains("-next")) {
				Assert.state(jobExplorer != null,
						"A JobExplorer must be provided for a restart or start next operation.  Please add one to the configuration.");
			}

			String jobName = jobIdentifier;
			
			JobParameters jobParameters = jobParametersConverter.getJobParameters(StringUtils
					.splitArrayElementsIntoProperties(parameters, "="));
			Assert.isTrue(parameters == null || parameters.length == 0 || !jobParameters.isEmpty(),
					"Invalid JobParameters " + Arrays.asList(parameters)
					+ ". If parameters are provided they should be in the form name=value (no whitespace).");

			if (opts.contains("-stop")) {
				List<JobExecution> jobExecutions = getRunningJobExecutions(jobIdentifier);
				if (jobExecutions == null) {
					throw new JobExecutionNotRunningException("No running execution found for job=" + jobIdentifier);
				}
				for (JobExecution jobExecution : jobExecutions) {
					jobExecution.setStatus(BatchStatus.STOPPING);
					jobRepository.update(jobExecution);
				}
				return exitCodeMapper.intValue(ExitStatus.COMPLETED.getExitCode());
			}

			if (opts.contains("-abandon")) {
				List<JobExecution> jobExecutions = getStoppedJobExecutions(jobIdentifier);
				if (jobExecutions == null) {
					throw new JobExecutionNotStoppedException("No stopped execution found for job=" + jobIdentifier);
				}
				for (JobExecution jobExecution : jobExecutions) {
					jobExecution.setStatus(BatchStatus.ABANDONED);
					jobRepository.update(jobExecution);
				}
				return exitCodeMapper.intValue(ExitStatus.COMPLETED.getExitCode());
			}

			if (opts.contains("-restart")) {
				JobExecution jobExecution = getLastFailedJobExecution(jobIdentifier);
				if (jobExecution == null) {
					throw new JobExecutionNotFailedException("No failed or stopped execution found for job="
							+ jobIdentifier);
				}
				jobParameters = jobExecution.getJobParameters();
				jobName = jobExecution.getJobInstance().getJobName();
			}

			Job job = null;
			if (jobLocator != null) {
				try {
					job = jobLocator.getJob(jobName);
				} catch (NoSuchJobException e) {
				}
			}
			if (job == null) {
				job = (Job) context.getBean(jobName);
			}

			if (opts.contains("-next")) {
				jobParameters = new JobParametersBuilder(jobParameters, jobExplorer)
						.getNextJobParameters(job)
						.toJobParameters();
			}

			JobExecution jobExecution = launcher.run(job, jobParameters);
			return exitCodeMapper.intValue(jobExecution.getExitStatus().getExitCode());

		}
		catch (Throwable e) {
			String message = "Job Terminated in error: " + e.getMessage();
			logger.error(message, e);
			CommandLineJobRunner.message = message;
			return exitCodeMapper.intValue(ExitStatus.FAILED.getExitCode());
		}
		finally {
			if (context != null) {
				context.close();
			}
		}
	}

	/**
	 * @param jobIdentifier a job execution id or job name
	 * @param minStatus the highest status to exclude from the result
	 * @return
	 */
	private List<JobExecution> getJobExecutionsWithStatusGreaterThan(String jobIdentifier, BatchStatus minStatus) {

		Long executionId = getLongIdentifier(jobIdentifier);
		if (executionId != null) {
			JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
			if (jobExecution.getStatus().isGreaterThan(minStatus)) {
				return Arrays.asList(jobExecution);
			}
			return Collections.emptyList();
		}

		int start = 0;
		int count = 100;
		List<JobExecution> executions = new ArrayList<>();
		List<JobInstance> lastInstances = jobExplorer.getJobInstances(jobIdentifier, start, count);

		while (!lastInstances.isEmpty()) {

			for (JobInstance jobInstance : lastInstances) {
				List<JobExecution> jobExecutions = jobExplorer.getJobExecutions(jobInstance);
				if (jobExecutions == null || jobExecutions.isEmpty()) {
					continue;
				}
				for (JobExecution jobExecution : jobExecutions) {
					if (jobExecution.getStatus().isGreaterThan(minStatus)) {
						executions.add(jobExecution);
					}
				}
			}

			start += count;
			lastInstances = jobExplorer.getJobInstances(jobIdentifier, start, count);

		}

		return executions;

	}

	private JobExecution getLastFailedJobExecution(String jobIdentifier) {
		List<JobExecution> jobExecutions = getJobExecutionsWithStatusGreaterThan(jobIdentifier, BatchStatus.STOPPING);
		if (jobExecutions.isEmpty()) {
			return null;
		}
		return jobExecutions.get(0);
	}

	private List<JobExecution> getStoppedJobExecutions(String jobIdentifier) {
		List<JobExecution> jobExecutions = getJobExecutionsWithStatusGreaterThan(jobIdentifier, BatchStatus.STARTED);
		if (jobExecutions.isEmpty()) {
			return null;
		}
		List<JobExecution> result = new ArrayList<>();
		for (JobExecution jobExecution : jobExecutions) {
			if (jobExecution.getStatus() != BatchStatus.ABANDONED) {
				result.add(jobExecution);
			}
		}
		return result.isEmpty() ? null : result;
	}

	private List<JobExecution> getRunningJobExecutions(String jobIdentifier) {
		Long executionId = getLongIdentifier(jobIdentifier);
		List<JobExecution> result = new ArrayList<>();
		if (executionId != null) {
			JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
			if (jobExecution != null && jobExecution.isRunning()) {
				result.add(jobExecution);
			}
		}
		else {
			result.addAll(jobExplorer.findRunningJobExecutions(jobIdentifier));
		}
		return result.isEmpty() ? null : result;
	}

	private Long getLongIdentifier(String jobIdentifier) {
		try {
			return new Long(jobIdentifier);
		}
		catch (NumberFormatException e) {
			// Not an ID - must be a name
			return null;
		}
	}

	/**
	 * Launch a batch job using a {@link CommandLineJobRunner}. Creates a new
	 * Spring context for the job execution, and uses a common parent for all
	 * such contexts. No exception are thrown from this method, rather
	 * exceptions are logged and an integer returned through the exit status in
	 * a {@link JvmSystemExiter} (which can be overridden by defining one in the
	 * Spring context).<br>
	 * Parameters can be provided in the form key=value, and will be converted
	 * using the injected {@link JobParametersConverter}.
	 *
	 * @param args
	 * <ul>
	 * <li>-restart: (optional) if the job has failed or stopped and the most
	 * should be restarted. If specified then the jobIdentifier parameter can be
	 * interpreted either as the name of the job or the id of the job execution
	 * that failed.</li>
	 * <li>-next: (optional) if the job has a {@link JobParametersIncrementer}
	 * that can be used to launch the next instance in a sequence</li>
	 * <li>jobPath: the xml application context containing a {@link Job}
	 * <li>jobIdentifier: the bean id of the job or id of the failed execution
	 * in the case of a restart.
	 * <li>jobParameters: 0 to many parameters that will be used to launch a
	 * job.
	 * </ul>
	 * <p>
	 * The options (<code>-restart, -next</code>) can occur anywhere in the
	 * command line.
	 * </p>
	 *
	 * @throws Exception is thrown if error occurs.
	 */
	public static void main(String[] args) throws Exception {

		CommandLineJobRunner command = new CommandLineJobRunner();

		List<String> newargs = new ArrayList<>(Arrays.asList(args));

		try {
			if (System.in.available() > 0) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
				String line = " ";
				while (line != null) {
					if (!line.startsWith("#") && StringUtils.hasText(line)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Stdin arg: " + line);
						}
						newargs.add(line);
					}
					line = reader.readLine();
				}
			}
		}
		catch (IOException e) {
			logger.warn("Could not access stdin (maybe a platform limitation)");
			if (logger.isDebugEnabled()) {
				logger.debug("Exception details", e);
			}
		}

		Set<String> opts = new LinkedHashSet<>();
		List<String> params = new ArrayList<>();

		int count = 0;
		String jobPath = null;
		String jobIdentifier = null;

		for (String arg : newargs) {
			if (VALID_OPTS.contains(arg)) {
				opts.add(arg);
			}
			else {
				switch (count) {
				case 0:
					jobPath = arg;
					break;
				case 1:
					jobIdentifier = arg;
					break;
				default:
					params.add(arg);
					break;
				}
				count++;
			}
		}

		if (jobPath == null || jobIdentifier == null) {
			String message = "At least 2 arguments are required: JobPath/JobClass and jobIdentifier.";
			logger.error(message);
			CommandLineJobRunner.message = message;
			command.exit(1);
			return;
		}

		String[] parameters = params.toArray(new String[params.size()]);

		int result = command.start(jobPath, jobIdentifier, parameters, opts);
		command.exit(result);
	}

}
