/*
 * Copyright 2025 the original author or authors.
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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.log.LogAccessor;
import org.springframework.util.Assert;

import static org.springframework.batch.core.launch.support.ExitCodeMapper.JVM_EXITCODE_COMPLETED;
import static org.springframework.batch.core.launch.support.ExitCodeMapper.JVM_EXITCODE_GENERIC_ERROR;

/**
 * A command-line utility to operate Spring Batch jobs using the {@link JobOperator}. It
 * allows starting, stopping, restarting, abandoning and recovering jobs from the command
 * line.
 * <p>
 * This utility requires a Spring application context to be set up with the necessary
 * batch infrastructure, including a {@link JobOperator}, a {@link JobRepository}, and a
 * {@link JobRegistry} populated with the jobs to operate. It can also be configured with
 * a custom {@link ExitCodeMapper} and a {@link JobParametersConverter}.
 *
 * <p>
 * This class is designed to be run from the command line, and the Javadoc of the
 * {@link #main(String[])} method explains the various operations and exit codes.
 *
 * @author Mahmoud Ben Hassine
 * @author Yejeong Ham
 * @since 6.0
 */
public class CommandLineJobOperator {

	private static final LogAccessor logger = new LogAccessor(CommandLineJobOperator.class);

	private final JobOperator jobOperator;

	private final JobRepository jobRepository;

	private final JobRegistry jobRegistry;

	private ExitCodeMapper exitCodeMapper = new SimpleJvmExitCodeMapper();

	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();

	/**
	 * Create a new {@link CommandLineJobOperator} instance.
	 * @param jobOperator the {@link JobOperator} to use for job operations
	 * @param jobRepository the {@link JobRepository} to use for job meta-data management
	 * @param jobRegistry the {@link JobRegistry} to use for job lookup by name
	 */
	public CommandLineJobOperator(JobOperator jobOperator, JobRepository jobRepository, JobRegistry jobRegistry) {
		this.jobOperator = jobOperator;
		this.jobRepository = jobRepository;
		this.jobRegistry = jobRegistry;
	}

	/**
	 * Set the {@link JobParametersConverter} to use for converting command line
	 * parameters to {@link JobParameters}. Defaults to a
	 * {@link DefaultJobParametersConverter}.
	 * @param jobParametersConverter the job parameters converter to set
	 */
	public void setJobParametersConverter(JobParametersConverter jobParametersConverter) {
		this.jobParametersConverter = jobParametersConverter;
	}

	/**
	 * Set the {@link ExitCodeMapper} to use for converting job exit codes to JVM exit
	 * codes. Defaults to a {@link SimpleJvmExitCodeMapper}.
	 * @param exitCodeMapper the exit code mapper to set
	 */
	public void setExitCodeMapper(ExitCodeMapper exitCodeMapper) {
		this.exitCodeMapper = exitCodeMapper;
	}

	/**
	 * Start a job with the given name and parameters.
	 * @param jobName the name of the job to start
	 * @param parameters the parameters for the job
	 * @return the exit code of the job execution, or JVM_EXITCODE_GENERIC_ERROR if an
	 * error occurs
	 */
	public int start(String jobName, Properties parameters) {
		logger.info(() -> "Starting job with name '" + jobName + "' and parameters: " + parameters);
		try {
			Job job = this.jobRegistry.getJob(jobName);
			if (job == null) {
				logger.error(() -> "Unable to find job " + jobName + " in the job registry");
				return JVM_EXITCODE_GENERIC_ERROR;
			}
			JobParameters jobParameters = this.jobParametersConverter.getJobParameters(parameters);
			JobExecution jobExecution = this.jobOperator.start(job, jobParameters);
			return this.exitCodeMapper.intValue(jobExecution.getExitStatus().getExitCode());
		}
		catch (Exception e) {
			return JVM_EXITCODE_GENERIC_ERROR;
		}
	}

	/**
	 * Start the next instance of the job with the given name.
	 * @param jobName the name of the job to start
	 * @return the exit code of the job execution, or JVM_EXITCODE_GENERIC_ERROR if an
	 * error occurs
	 */
	public int startNextInstance(String jobName) {
		logger.info(() -> "Starting next instance of job '" + jobName + "'");
		try {
			Job job = this.jobRegistry.getJob(jobName);
			if (job == null) {
				logger.error(() -> "Unable to find job " + jobName + " in the job registry");
				return JVM_EXITCODE_GENERIC_ERROR;
			}
			JobExecution jobExecution = this.jobOperator.startNextInstance(job);
			return this.exitCodeMapper.intValue(jobExecution.getExitStatus().getExitCode());
		}
		catch (Exception e) {
			return JVM_EXITCODE_GENERIC_ERROR;
		}
	}

	/**
	 * Send a stop signal to the job execution with given ID. The signal is successfully
	 * sent if this method returns JVM_EXITCODE_COMPLETED, but that doesn't mean that the
	 * job has stopped. The only way to be sure of that is to poll the job execution
	 * status.
	 * @param jobExecutionId the ID of the job execution to stop
	 * @return JVM_EXITCODE_COMPLETED if the stop signal was successfully sent to the job
	 * execution, JVM_EXITCODE_GENERIC_ERROR otherwise
	 * @see JobOperator#stop(JobExecution)
	 */
	public int stop(long jobExecutionId) {
		logger.info(() -> "Stopping job execution with ID: " + jobExecutionId);
		try {
			JobExecution jobExecution = this.jobRepository.getJobExecution(jobExecutionId);
			if (jobExecution == null) {
				logger.error(() -> "No job execution found with ID: " + jobExecutionId);
				return JVM_EXITCODE_GENERIC_ERROR;
			}
			boolean stopSignalSent = this.jobOperator.stop(jobExecution);
			return stopSignalSent ? JVM_EXITCODE_COMPLETED : JVM_EXITCODE_GENERIC_ERROR;
		}
		catch (Exception e) {
			return JVM_EXITCODE_GENERIC_ERROR;
		}
	}

	/**
	 * Restart the job execution with the given ID.
	 * @param jobExecutionId the ID of the job execution to restart
	 * @return the exit code of the restarted job execution, or JVM_EXITCODE_GENERIC_ERROR
	 * if an error occurs
	 */
	public int restart(long jobExecutionId) {
		logger.info(() -> "Restarting job execution with ID: " + jobExecutionId);
		try {
			JobExecution jobExecution = this.jobRepository.getJobExecution(jobExecutionId);
			if (jobExecution == null) {
				logger.error(() -> "No job execution found with ID: " + jobExecutionId);
				return JVM_EXITCODE_GENERIC_ERROR;
			}
			// TODO should check and log error if the job execution did not fail
			JobExecution restartedExecution = this.jobOperator.restart(jobExecution);
			return this.exitCodeMapper.intValue(restartedExecution.getExitStatus().getExitCode());
		}
		catch (Exception e) {
			return JVM_EXITCODE_GENERIC_ERROR;
		}
	}

	/**
	 * Abandon the job execution with the given ID.
	 * @param jobExecutionId the ID of the job execution to abandon
	 * @return the exit code of the abandoned job execution, or JVM_EXITCODE_GENERIC_ERROR
	 * if an error occurs
	 */
	public int abandon(long jobExecutionId) {
		logger.info(() -> "Abandoning job execution with ID: " + jobExecutionId);
		try {
			JobExecution jobExecution = this.jobRepository.getJobExecution(jobExecutionId);
			if (jobExecution == null) {
				logger.error(() -> "No job execution found with ID: " + jobExecutionId);
				return JVM_EXITCODE_GENERIC_ERROR;
			}
			// TODO should throw JobExecutionNotStoppedException if the job execution is
			// not stopped
			JobExecution abandonedExecution = this.jobOperator.abandon(jobExecution);
			return this.exitCodeMapper.intValue(abandonedExecution.getExitStatus().getExitCode());
		}
		catch (Exception e) {
			return JVM_EXITCODE_GENERIC_ERROR;
		}
	}

	/**
	 * Recover the job execution with the given ID that is stuck in a {@code STARTED}
	 * state due to an abrupt shutdown or failure, making it eligible for restart.
	 * @param jobExecutionId the ID of the job execution to recover
	 * @return the exit code of the recovered job execution, or JVM_EXITCODE_GENERIC_ERROR
	 * if an error occurs
	 */
	public int recover(long jobExecutionId) {
		logger.info(() -> "Recovering job execution with ID: " + jobExecutionId);
		try {
			JobExecution jobExecution = this.jobRepository.getJobExecution(jobExecutionId);
			if (jobExecution == null) {
				logger.error(() -> "No job execution found with ID: " + jobExecutionId);
				return JVM_EXITCODE_GENERIC_ERROR;
			}
			JobExecution recoveredExecution = this.jobOperator.recover(jobExecution);
			return this.exitCodeMapper.intValue(recoveredExecution.getExitStatus().getExitCode());
		}
		catch (Exception e) {
			return JVM_EXITCODE_GENERIC_ERROR;
		}
	}

	// @formatter:off
	/**
	 * Main method to operate jobs from the command line.
	 * <p>
     * Usage:
     * <code>
	 * java org.springframework.batch.core.launch.support.CommandLineJobOperator \
	 *        fully.qualified.name.of.JobConfigurationClass \
	 *        operation \
	 *        parameters
	 * </code>
	 * <p>
     * where <code>operation</code> is one of the following:
     * <ul>
     *     <li>start jobName <code>[jobParameters]</code></li>
     *     <li>startNextInstance jobName</li>
     *     <li>restart jobExecutionId</li>
     *     <li>stop jobExecutionId</li>
     *     <li>abandon jobExecutionId</li>
     *     <li>recover jobExecutionId</li>
     * </ul>
	 * <p>
	 * and <code>jobParameters</code> are key-value pairs in the form name=value,type,identifying.
	 * <p>
	 * Exit status:
     * <ul>
     *     <li>0: Job completed successfully</li>
     *     <li>1: Job failed to (re)start or an error occurred</li>
     *     <li>2: Job configuration class not found</li>
     * </ul>
	 */
    // @formatter:on
	public static void main(String[] args) {
		if (args.length < 3) {
			String usage = """
					Usage: java %s <fully.qualified.name.of.JobConfigurationClass> <operation> <parameters>
					where operation is one of the following:
					 - start jobName [jobParameters]
					 - startNextInstance jobName
					 - restart jobExecutionId
					 - stop jobExecutionId
					 - abandon jobExecutionId
					 - recover jobExecutionId
					and jobParameters are key-value pairs in the form name=value,type,identifying.
					""";
			System.err.printf(String.format(usage, CommandLineJobOperator.class.getName()));
			System.exit(1);
		}

		String jobConfigurationClassName = args[0];
		String operation = args[1];

		ConfigurableApplicationContext context = null;
		try {
			Class<?> jobConfigurationClass = Class.forName(jobConfigurationClassName);
			context = new AnnotationConfigApplicationContext(jobConfigurationClass);
		}
		catch (ClassNotFoundException classNotFoundException) {
			System.err.println("Job configuration class not found: " + jobConfigurationClassName);
			System.exit(2);
		}

		Assert.notNull(context, "Application context must not be null");

		JobOperator jobOperator = null;
		JobRepository jobRepository = null;
		JobRegistry jobRegistry = null;
		try {
			jobOperator = context.getBean(JobOperator.class);
			jobRepository = context.getBean(JobRepository.class);
			jobRegistry = context.getBean(JobRegistry.class);
		}
		catch (BeansException e) {
			System.err.println("A required bean was not found in the application context: " + e.getMessage());
			System.exit(1);
		}

		Assert.notNull(jobOperator, "JobOperator must not be null");
		Assert.notNull(jobRepository, "JobRepository must not be null");
		Assert.notNull(jobRegistry, "JobRegistry must not be null");

		CommandLineJobOperator operator = new CommandLineJobOperator(jobOperator, jobRepository, jobRegistry);

		int exitCode;
		String jobName;
		long jobExecutionId;
		switch (operation) {
			case "start":
				jobName = args[2];
				List<String> jobParameters = Arrays.asList(args).subList(3, args.length);
				exitCode = operator.start(jobName, parse(jobParameters));
				break;
			case "startNextInstance":
				jobName = args[2];
				exitCode = operator.startNextInstance(jobName);
				break;
			case "stop":
				jobExecutionId = Long.parseLong(args[2]);
				exitCode = operator.stop(jobExecutionId);
				break;
			case "restart":
				jobExecutionId = Long.parseLong(args[2]);
				exitCode = operator.restart(jobExecutionId);
				break;
			case "abandon":
				jobExecutionId = Long.parseLong(args[2]);
				exitCode = operator.abandon(jobExecutionId);
				break;
			case "recover":
				jobExecutionId = Long.parseLong(args[2]);
				exitCode = operator.recover(jobExecutionId);
				break;
			default:
				System.err.println("Unknown operation: " + operation);
				exitCode = JVM_EXITCODE_GENERIC_ERROR;
		}

		System.exit(exitCode);
	}

	private static Properties parse(List<String> jobParameters) {
		Properties properties = new Properties();
		for (String jobParameter : jobParameters) {
			String[] tokens = jobParameter.split("=");
			properties.put(tokens[0], tokens[1]);
		}
		return properties;
	}

}
