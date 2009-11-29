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

package org.springframework.batch.test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.job.flow.FlowJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * <p>
 * Base class for testing batch jobs. It provides methods for launching an
 * entire {@link AbstractJob}, allowing for end to end testing of individual
 * steps, without having to run every step in the job. Any test classes
 * inheriting from this class should make sure they are part of an
 * {@link ApplicationContext}, which is generally expected to be done as part of
 * the Spring test framework. Furthermore, the {@link ApplicationContext} in
 * which it is a part of is expected to have one {@link JobLauncher},
 * {@link JobRepository}, and a single {@link AbstractJob} implementation.
 * 
 * <p>
 * This class also provides the ability to run {@link Step}s from a
 * {@link FlowJob} or {@link SimpleJob} individually. By launching {@link Step}s
 * within a {@link Job} on their own, end to end testing of individual steps can
 * be performed without having to run every step in the job.
 * 
 * <p>
 * It should be noted that using any of the methods that don't contain
 * {@link JobParameters} in their signature, will result in one being created
 * with the current system time as a parameter. This will ensure restartability
 * when no parameters are provided.
 * 
 * @author Lucas Ward
 * @author Dan Garrette
 * @since 2.0
 * 
 * @deprecated (from 2.1) use {@link JobLauncherTestUtils} instead
 */
public abstract class AbstractJobTests implements ApplicationContextAware {

	/** Logger */
	protected final Log logger = LogFactory.getLog(getClass());

	@Autowired
	private JobLauncher launcher;

	@Autowired
	private AbstractJob job;

	@Autowired
	private JobRepository jobRepository;

	private StepRunner stepRunner;

	private ApplicationContext applicationContext;

	/**
	 * {@inheritDoc}
	 */
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	/**
	 * @return the applicationContext
	 */
	protected ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	/**
	 * @return the job repository which is autowired by type
	 */
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	/**
	 * @return the job which is autowired by type
	 */
	public AbstractJob getJob() {
		return job;
	}

	/**
	 * @return the launcher
	 */
	protected JobLauncher getJobLauncher() {
		return launcher;
	}

	/**
	 * Launch the entire job, including all steps.
	 * 
	 * @return JobExecution, so that the test can validate the exit status
	 * @throws Exception
	 */
	protected JobExecution launchJob() throws Exception {
		return this.launchJob(this.getUniqueJobParameters());
	}

	/**
	 * Launch the entire job, including all steps
	 * 
	 * @param jobParameters
	 * @return JobExecution, so that the test can validate the exit status
	 * @throws Exception
	 */
	protected JobExecution launchJob(JobParameters jobParameters) throws Exception {
		return getJobLauncher().run(this.job, jobParameters);
	}

	/**
	 * @return a new JobParameters object containing only a parameter for the
	 * current timestamp, to ensure that the job instance will be unique.
	 */
	protected JobParameters getUniqueJobParameters() {
		Map<String, JobParameter> parameters = new HashMap<String, JobParameter>();
		parameters.put("timestamp", new JobParameter(new Date().getTime()));
		return new JobParameters(parameters);
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
	public JobExecution launchStep(String stepName, JobParameters jobParameters, ExecutionContext jobExecutionContext) {
		Step step = this.job.getStep(stepName);
		if (step == null) {
			step = this.job.getStep(this.job.getName() + "." + stepName);
		}
		if (step == null) {
			throw new IllegalStateException("No Step found with name: [" + stepName + "]");
		}
		return getStepRunner().launchStep(step, jobParameters, jobExecutionContext);
	}
}
