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

package org.springframework.batch.test;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

/**
 * <p>
 * Utility class for testing batch jobs. It provides methods for launching an
 * entire {@link AbstractJob}, allowing for end to end testing of individual
 * steps, without having to run every step in the job. Any test classes using
 * this utility can set up an instance in the {@link ApplicationContext} as part
 * of the Spring test framework.
 * </p>
 * 
 * <p>
 * This class also provides the ability to run {@link Step}s from a
 * {@link FlowJob} or {@link SimpleJob} individually. By launching {@link Step}s
 * within a {@link Job} on their own, end to end testing of individual steps can
 * be performed without having to run every step in the job.
 * </p>
 * 
 * <p>
 * It should be noted that using any of the methods that don't contain
 * {@link JobParameters} in their signature, will result in one being created
 * with a random number of type {@code long} as a parameter. This will ensure
 * restartability when no parameters are provided.
 * </p>
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @since 2.1
 */
public class JobLauncherTestUtils {

	private SecureRandom secureRandom = new SecureRandom();

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	private JobLauncher jobLauncher;

	private Job job;

	private JobRepository jobRepository;

	private StepRunner stepRunner;

	/**
	 * The Job instance that can be manipulated (e.g. launched) in this utility.
	 * 
	 * @param job the {@link AbstractJob} to use
	 */
	@Autowired
	public void setJob(Job job) {
		this.job = job;
	}

	/**
	 * The {@link JobRepository} to use for creating new {@link JobExecution}
	 * instances.
	 * 
	 * @param jobRepository a {@link JobRepository}
	 */
	@Autowired
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * @return the job repository
	 */
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	/**
	 * @return the job
	 */
	public Job getJob() {
		return job;
	}

	/**
	 * A {@link JobLauncher} instance that can be used to launch jobs.
	 * 
	 * @param jobLauncher a job launcher
	 */
	@Autowired
	public void setJobLauncher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	/**
	 * @return the job launcher
	 */
	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	/**
	 * Launch the entire job, including all steps.
	 * 
	 * @return JobExecution, so that the test can validate the exit status
	 * @throws Exception thrown if error occurs launching the job.
	 */
	public JobExecution launchJob() throws Exception {
		return this.launchJob(this.getUniqueJobParameters());
	}

	/**
	 * Launch the entire job, including all steps
	 * 
	 * @param jobParameters instance of {@link JobParameters}.
	 * @return JobExecution, so that the test can validate the exit status
	 * @throws Exception thrown if error occurs launching the job.
	 */
	public JobExecution launchJob(JobParameters jobParameters) throws Exception {
		return getJobLauncher().run(this.job, jobParameters);
	}

	/**
	 * @return a new JobParameters object containing only a parameter with a
	 * random number of type {@code long}, to ensure that the job instance will be unique.
	 */
	public JobParameters getUniqueJobParameters() {
		Map<String, JobParameter> parameters = new HashMap<>();
		parameters.put("random", new JobParameter(this.secureRandom.nextLong()));
		return new JobParameters(parameters);
	}

    /**
     * @return a new JobParametersBuilder object containing only a parameter with a
     * random number of type {@code long}, to ensure that the job instance will be unique.
     */
    public JobParametersBuilder getUniqueJobParametersBuilder() {
        return new JobParametersBuilder(this.getUniqueJobParameters());
    }

	/**
	 * Convenient method for subclasses to grab a {@link StepRunner} for running
	 * steps by name.
	 * 
	 * @return a {@link StepRunner}
	 */
	protected StepRunner getStepRunner() {
		if (this.stepRunner == null) {
			this.stepRunner = new StepRunner(getJobLauncher(), getJobRepository());
		}
		return this.stepRunner;
	}

	/**
	 * Launch just the specified step in the job. A unique set of JobParameters
	 * will automatically be generated. An IllegalStateException is thrown if
	 * there is no Step with the given name.
	 * 
	 * @param stepName The name of the step to launch
	 * @return JobExecution
	 */
	public JobExecution launchStep(String stepName) {
		return this.launchStep(stepName, this.getUniqueJobParameters(), null);
	}

	/**
	 * Launch just the specified step in the job. A unique set of JobParameters
	 * will automatically be generated. An IllegalStateException is thrown if
	 * there is no Step with the given name.
	 * 
	 * @param stepName The name of the step to launch
	 * @param jobExecutionContext An ExecutionContext whose values will be
	 * loaded into the Job ExecutionContext prior to launching the step.
	 * @return JobExecution
	 */
	public JobExecution launchStep(String stepName, ExecutionContext jobExecutionContext) {
		return this.launchStep(stepName, this.getUniqueJobParameters(), jobExecutionContext);
	}

	/**
	 * Launch just the specified step in the job. An IllegalStateException is
	 * thrown if there is no Step with the given name.
	 * 
	 * @param stepName The name of the step to launch
	 * @param jobParameters The JobParameters to use during the launch
	 * @return JobExecution
	 */
	public JobExecution launchStep(String stepName, JobParameters jobParameters) {
		return this.launchStep(stepName, jobParameters, null);
	}

	/**
	 * Launch just the specified step in the job. An IllegalStateException is
	 * thrown if there is no Step with the given name.
	 * 
	 * @param stepName The name of the step to launch
	 * @param jobParameters The JobParameters to use during the launch
	 * @param jobExecutionContext An ExecutionContext whose values will be
	 * loaded into the Job ExecutionContext prior to launching the step.
	 * @return JobExecution
	 */
	public JobExecution launchStep(String stepName, JobParameters jobParameters, @Nullable ExecutionContext jobExecutionContext) {
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
		return getStepRunner().launchStep(step, jobParameters, jobExecutionContext);
	}
}
