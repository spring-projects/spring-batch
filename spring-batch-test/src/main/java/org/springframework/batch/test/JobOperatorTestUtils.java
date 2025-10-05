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
package org.springframework.batch.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.job.UnexpectedJobExecutionException;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.launch.JobRestartException;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;

/**
 * <p>
 * Utility class for testing batch jobs. It provides methods for starting an entire
 * {@link AbstractJob}, allowing for end-to-end testing of individual steps, without
 * having to run every step in the job. Any test classes using this utility can set up an
 * instance in the {@link ApplicationContext} as part of the Spring test framework. The
 * test context must contain batch infrastructure beans (ie a {@link JobRepository} and a
 * {@link JobOperator}) as well as the job under test. The job under test will be
 * autowired into this utility.
 * </p>
 *
 * <p>
 * This class also provides the ability to run {@link Step}s individually from a
 * {@link SimpleJob} {@link FlowJob}. By starting {@link Step}s within a {@link Job} on
 * their own, end-to-end testing of individual steps can be performed without having to
 * run every step in the job.
 * </p>
 *
 * <p>
 * It should be noted that using any of the methods that don't contain
 * {@link JobParameters} in their signature, will result in one being created with a
 * random number of type {@code long} as a parameter. This will ensure restartability when
 * no parameters are provided.
 * </p>
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 *
 */
@SuppressWarnings("removal")
public class JobOperatorTestUtils extends JobLauncherTestUtils {

	/**
	 * Name of the single-step job surrounding steps when tested individually
	 */
	public static final String JOB_NAME = "TestJob";

	protected JobOperator jobOperator;

	/**
	 * Create a new instance of {@link JobOperatorTestUtils} with the provided job
	 * repository and job operator.
	 * @param jobOperator to use to start jobs and steps
	 * @param jobRepository to use to access job metadata
	 */
	public JobOperatorTestUtils(JobOperator jobOperator, JobRepository jobRepository) {
		Assert.notNull(jobOperator, "JobRepository must not be null");
		Assert.notNull(jobRepository, "JobRepository must not be null");
		this.jobOperator = jobOperator;
		this.jobRepository = jobRepository;
	}

	/**
	 * Set the job that can be operated by this utility.
	 * @param job the job to test
	 */
	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * Set the job operator to be used by this utility.
	 * @param jobOperator the job operator to use to start jobs and steps
	 */
	public void setJobOperator(JobOperator jobOperator) {
		this.jobOperator = jobOperator;
	}

	/**
	 * Set the job repository to be used by this utility.
	 * @param jobRepository the job repository to use to access job metadata
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Start the entire job, including all steps, with a set of unique random job
	 * parameters.
	 * @return JobExecution, so that the test can validate the exit status
	 * @throws Exception thrown if error occurs launching the job.
	 */
	public JobExecution startJob() throws Exception {
		return this.startJob(super.getUniqueJobParameters());
	}

	/**
	 * Start the entire job, including all steps, with the provided set of job parameters.
	 * @param jobParameters instance of {@link JobParameters}.
	 * @return JobExecution, so that the test can validate the exit status
	 * @throws Exception thrown if error occurs launching the job.
	 */
	public JobExecution startJob(JobParameters jobParameters) throws Exception {
		return this.jobOperator.start(this.job, jobParameters);
	}

	/**
	 * Start just the specified step in a surrounding single-step job of type
	 * {@link SimpleJob} named {@link #JOB_NAME}. A unique set of JobParameters will
	 * automatically be generated. An IllegalStateException is thrown if there is no Step
	 * with the given name.
	 * @param stepName The name of the step to launch
	 * @return JobExecution
	 */
	public JobExecution startStep(String stepName) {
		return this.startStep(stepName, this.getUniqueJobParameters(), new ExecutionContext());
	}

	/**
	 * Extract the step from the injected job and start it in a surrounding single-step
	 * job of type {@link SimpleJob} named {@link #JOB_NAME}. An IllegalStateException is
	 * thrown if there is no Step with the given name.
	 * @param stepName The name of the step to start
	 * @param jobParameters The JobParameters to use during the start
	 * @param jobExecutionContext An ExecutionContext whose values will be loaded into the
	 * Job ExecutionContext before starting the step.
	 * @return JobExecution
	 */
	public JobExecution startStep(String stepName, JobParameters jobParameters, ExecutionContext jobExecutionContext) {
		if (!(job instanceof StepLocator)) {
			throw new UnsupportedOperationException("Cannot locate step from a Job that is not a StepLocator: job="
					+ job.getName() + " does not implement StepLocator");
		}
		StepLocator locator = (StepLocator) this.job;
		Step step = locator.getStep(stepName);
		if (step == null) {
			step = locator.getStep(this.job.getName() + "." + stepName);
		}
		if (step == null) {
			throw new IllegalStateException("No Step found with name: [" + stepName + "]");
		}

		return startStep(step, jobParameters, jobExecutionContext);
	}

	/**
	 * Start just the specified step with a unique set of job parameters in a surrounding
	 * single-step job of type {@link SimpleJob} named {@link StepRunner#JOB_NAME}. An
	 * IllegalStateException is thrown if there is no Step with the given name.
	 * @param step The step to start
	 * @return JobExecution
	 */
	public JobExecution startStep(Step step) {
		return startStep(step, getUniqueJobParameters(), new ExecutionContext());
	}

	/**
	 * Start just the specified step in a surrounding single-step job of type
	 * {@link SimpleJob} named {@link StepRunner#JOB_NAME}. An IllegalStateException is
	 * thrown if there is no Step with the given name.
	 * @param step The step to start
	 * @param jobParameters The JobParameters to use during the start
	 * @param jobExecutionContext An ExecutionContext whose values will be loaded into the
	 * Job ExecutionContext before starting the step.
	 * @return JobExecution
	 */
	public JobExecution startStep(Step step, JobParameters jobParameters, ExecutionContext jobExecutionContext) {
		// Create a fake job
		SimpleJob job = new SimpleJob();
		job.setName(JOB_NAME);
		job.setJobRepository(this.jobRepository);

		List<Step> stepsToExecute = new ArrayList<>();
		stepsToExecute.add(step);
		job.setSteps(stepsToExecute);

		// Dump the given Job ExecutionContext using a listener
		if (jobExecutionContext != null && !jobExecutionContext.isEmpty()) {
			job.setJobExecutionListeners(new JobExecutionListener[] { new JobExecutionListener() {
				@Override
				public void beforeJob(JobExecution jobExecution) {
					ExecutionContext jobContext = jobExecution.getExecutionContext();
					for (Map.Entry<String, Object> entry : jobExecutionContext.entrySet()) {
						jobContext.put(entry.getKey(), entry.getValue());
					}
				}
			} });
		}

		// Launch the job
		try {
			return this.jobOperator.start(job, jobParameters);
		}
		catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| InvalidJobParametersException e) {
			throw new UnexpectedJobExecutionException("Step runner encountered exception.", e);
		}
	}

	/**
	 * @return a new {@link JobParameters} object containing only a parameter with a
	 * random number of type {@code long}, to ensure that the job instance will be unique.
	 */
	public JobParameters getUniqueJobParameters() {
		return super.getUniqueJobParameters();
	}

	/**
	 * @return a new {@link JobParametersBuilder} object containing only a parameter with
	 * a random number of type {@code long}, to ensure that the job instance will be
	 * unique.
	 */
	public JobParametersBuilder getUniqueJobParametersBuilder() {
		return super.getUniqueJobParametersBuilder();
	}

}
