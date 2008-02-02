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

package org.springframework.batch.execution.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobSupport;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.repository.BatchRestartException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.repository.dao.JobDao;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.item.StreamContext;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.Assert;

/**
 * 
 * <p>
 * Simple Job Repository that stores Jobs, JobExecutions, Steps, and
 * StepExecutions using the provided JobDao and StepDao.
 * <p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @see JobRepository
 * @see StepDao
 * @see JobDao
 * 
 */
public class SimpleJobRepository implements JobRepository {

	private JobDao jobDao;

	private StepDao stepDao;

	/**
	 * Provide default constructor with low visibility in case user wants to use
	 * use aop:proxy-target-class="true" for transaction interceptor.
	 */
	SimpleJobRepository() {
	}

	public SimpleJobRepository(JobDao jobDao, StepDao stepDao) {
		super();
		this.jobDao = jobDao;
		this.stepDao = stepDao;
	}

	/**
	 * <p>
	 * Create a (@link {@link JobExecution}) based on the passed in
	 * {@link JobIdentifier} and {@link JobSupport}. However, unique identification of
	 * a job can only come from the database, and therefore must come from
	 * JobDao by either creating a new job or finding an existing one, which
	 * will ensure that the id of the job is populated with the correct value.
	 * </p>
	 * 
	 * <p>
	 * There are two ways in which the method determines if a job should be
	 * created or an existing one should be returned. The first is
	 * restartability. The {@link JobSupport} restartable property will be checked
	 * first. If it is not false, a new job will be created, regardless of
	 * whether or not one exists. If it is true, the {@link JobDao} will be
	 * checked to determine if the job already exists, if it does, it's steps
	 * will be populated (there must be at least 1) and a new
	 * {@link JobExecution} will be returned. If no job is found, a new one will
	 * be created based on the job.
	 * </p>
	 * 
	 * <p>
	 * A check is made to see if any job executions are already running, and an
	 * exception will be thrown if one is detected. To detect a running job
	 * execution we use the {@link JobDao}:
	 * <ol>
	 * <li>First we find all jobs which match the given {@link JobIdentifier}</li>
	 * <li>What happens then depends on how many existing job instances we
	 * find:
	 * <ul>
	 * <li>If there are none, or the {@link JobSupport} is marked restartable, then we
	 * create a new {@link JobInstance}</li>
	 * <li>If there is more than one and the {@link JobSupport} is not marked as
	 * restartable, it is an error. This could be caused by a job whose
	 * restartable flag has changed to be more strict (true not false)
	 * <em>after</em> it has been executed at least once.</li>
	 * <li>If there is precisely one existing {@link JobInstance} then we check
	 * the {@link JobExecution} instances for that job, and if any of them tells
	 * us it is running (see {@link JobExecution#isRunning()}) then it is an
	 * error.</li>
	 * </ul>
	 * </li>
	 * </ol>
	 * If this method is run in a transaction (as it normally would be) with
	 * isolation level at {@link Isolation#REPEATABLE_READ} or better, then this
	 * method should block if another transaction is already executing it (for
	 * the same {@link JobIdentifier}). The first transaction to complete in
	 * this scenario obtains a valid {@link JobExecution}, and others throw
	 * {@link JobExecutionAlreadyRunningException} (or timeout). There are no
	 * such guarantees if the {@link JobDao} does not respect the transaction
	 * isolation levels (e.g. if using a non-relational data-store, or if the
	 * platform does not support the higher isolation levels).
	 * </p>
	 * 
	 * @see JobRepository#createJobExecution(JobSupport, JobParameters)
	 * 
	 * @throws BatchRestartException if more than one JobInstance if found or if
	 * JobInstance.getJobExecutionCount() is greater than Job.getStartLimit()
	 * @throws JobExecutionAlreadyRunningException if a job execution is found
	 * for the given {@link JobIdentifier} that is already running
	 * 
	 */
	public JobExecution createJobExecution(Job job, JobParameters jobParameters)
			throws JobExecutionAlreadyRunningException {

		Assert.notNull(job, "Job must not be null.");
		Assert.notNull(jobParameters, "JobParameters must not be null.");

		List jobs = new ArrayList();
		JobInstance jobInstance;

		// Check if a job is restartable, if not, create and return a new job
		if (job.isRestartable()) {

			/*
			 * Find all jobs matching the runtime information.
			 * 
			 * Always do this if the job is restartable, then if this method is
			 * transactional, and the isolation level is REPEATABLE_READ or
			 * better, another launcher trying to start the same job in another
			 * thread or process will block until this transaction has finished.
			 */

			jobs = jobDao.findJobInstances(job.getName(), jobParameters);
		}

		if (jobs.size() == 1) {
			// One job was found
			jobInstance = (JobInstance) jobs.get(0);
			jobInstance.setStepInstances(findStepInstances(job.getSteps(), jobInstance));
			jobInstance.setJobExecutionCount(jobDao.getJobExecutionCount(jobInstance.getId()));
			if (jobInstance.getJobExecutionCount() > job.getStartLimit()) {
				throw new BatchRestartException("Restart Max exceeded for Job: " + jobInstance.toString());
			}
			List executions = jobDao.findJobExecutions(jobInstance);
			for (Iterator iterator = executions.iterator(); iterator.hasNext();) {
				JobExecution execution = (JobExecution) iterator.next();
				if (execution.isRunning()) {
					throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: "
							+ jobInstance);
				}
			}
		}
		else if (jobs.size() == 0) {
			// no job found, create one
			jobInstance = createJobInstance(job, jobParameters);
		}
		else {
			// More than one job found, throw exception
			throw new BatchRestartException("Error restarting job, more than one JobInstance found for: "
					+ job.toString());
		}

		return generateJobExecution(jobInstance);

	}

	private JobExecution generateJobExecution(JobInstance job) {
		JobExecution execution = job.createJobExecution();
		// Save the JobExecution so that it picks up an ID (useful for clients
		// monitoring asynchronous executions):
		saveOrUpdate(execution);
		return execution;
	}

	/**
	 * Save or Update a JobExecution. A JobExecution is considered one
	 * 'execution' of a particular job. Therefore, it must have it's jobId field
	 * set before it is passed into this method. It also has it's own unique
	 * identifer, because it must be updatable separately. If an id isn't found,
	 * a new JobExecution is created, if one is found, the current row is
	 * updated.
	 * 
	 * @param JobExecution to be stored.
	 * @throws IllegalArgumentException if jobExecution is null.
	 */
	public void saveOrUpdate(JobExecution jobExecution) {

		Assert.notNull(jobExecution, "JobExecution cannot be null.");
		Assert.notNull(jobExecution.getJobId(), "JobExecution must have a Job ID set.");

		if (jobExecution.getId() == null) {
			// existing instance
			jobDao.save(jobExecution);
		}
		else {
			// new execution
			jobDao.update(jobExecution);
		}
	}

	/**
	 * Update an existing job. A job must have been obtained from the
	 * findOrCreateJob method, otherwise it is likely that the id is incorrect
	 * or non-existant.
	 * 
	 * @param job to be updated.
	 * @throws IllegalArgumentException if Job or it's Id is null.
	 */
	public void update(JobInstance job) {

		Assert.notNull(job, "Job cannot be null.");
		Assert.notNull(job.getId(), "Job cannot be updated if it's ID is null.  It must be obtained"
				+ "from SimpleJobRepository.findOrCreateJob to be considered valid.");

		jobDao.update(job);
	}

	/**
	 * Save or Update the given StepExecution. If it's id is null, it will be
	 * saved and an id will be set, otherwise it will be updated. It should be
	 * noted that assigning an ID randomly will likely cause an exception
	 * depending on the StepDao implementation.
	 * 
	 * @param StepExecution to be saved.
	 * @throws IllegalArgumentException if stepExecution is null.
	 */
	public void saveOrUpdate(StepExecution stepExecution) {

		Assert.notNull(stepExecution, "StepExecution cannot be null.");
		Assert.notNull(stepExecution.getStepId(), "StepExecution's Step Id cannot be null.");

		if (stepExecution.getId() == null) {
			// new execution, obtain id and insert
			stepDao.save(stepExecution);
		}
		else {
			// existing execution, update
			stepDao.update(stepExecution);
		}
	}

	/**
	 * Update the given step.
	 * 
	 * @param StepInstance to be updated.
	 * @throws IllegalArgumentException if step or it's id is null.
	 */
	public void update(StepInstance step) {

		Assert.notNull(step, "Step cannot be null.");
		Assert.notNull(step.getId(), "Step cannot be updated if it's ID is null.  It must be obtained"
				+ "from SimpleJobRepository.findOrCreateJob to be considered valid.");

		stepDao.update(step);

	}

	/*
	 * Convenience method for creating a new job. A new job is created by
	 * calling {@link JobDao#createJob(JobRuntimeInformation)} and then it's
	 * list of StepConfigurations is passed to the createSteps method.
	 */
	private JobInstance createJobInstance(Job job, JobParameters jobParameters) {

		JobInstance jobInstance = jobDao.createJobInstance(job.getName(), jobParameters);
		jobInstance.setJob(job);
		jobInstance.setStepInstances(createStepInstances(jobInstance, job.getSteps()));
		return jobInstance;
	}

	/*
	 * Create steps based on the given Job and list of Steps.
	 */
	private List createStepInstances(JobInstance job, List steps) {

		List stepInstances = new ArrayList();
		Iterator i = steps.iterator();
		while (i.hasNext()) {
			Step step = (Step) i.next();
			StepInstance stepInstance = stepDao.createStep(job, step.getName());
			// Ensure valid restart data is being returned.
			if (stepInstance.getStreamContext() == null || stepInstance.getStreamContext() == null) {
				stepInstance.setStreamContext(new StreamContext());
			}
			stepInstances.add(stepInstance);
		}

		return stepInstances;
	}

	/*
	 * Find Steps for the given list of Steps with a given JobId
	 */
	protected List findStepInstances(List steps, JobInstance job) {
		List stepInstances = new ArrayList();
		Iterator i = steps.iterator();
		while (i.hasNext()) {

			Step stepConfiguration = (Step) i.next();
			StepInstance step = stepDao.findStep(job, stepConfiguration.getName());
			if (step != null) {

				step.setStepExecutionCount(stepDao.getStepExecutionCount(step));
				// Ensure valid restart data is being returned.
				if (step.getStreamContext() == null || step.getStreamContext() == null) {
					step.setStreamContext(new StreamContext());
				}
				stepInstances.add(step);
			}
		}
		return stepInstances;
	}

}
