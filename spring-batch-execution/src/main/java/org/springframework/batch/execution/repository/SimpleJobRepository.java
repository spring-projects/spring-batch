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
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.repository.BatchRestartException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.repository.dao.JobExecutionDao;
import org.springframework.batch.execution.repository.dao.JobInstanceDao;
import org.springframework.batch.execution.repository.dao.StepExecutionDao;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.util.Assert;

/**
 * 
 * <p>
 * Implementation of {@link JobRepository} that stores JobInstances,
 * JobExecutions, StepInstances, and StepExecutions using the injected DAOs.
 * <p>
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 * @see JobRepository
 * @see JobInstanceDao
 * @see JobExecutionDao
 * @see StepInstanceDao
 * @see StepExecutionDao
 * 
 */
public class SimpleJobRepository implements JobRepository {

	private JobInstanceDao jobInstanceDao;

	private JobExecutionDao jobExecutionDao;

	private StepExecutionDao stepExecutionDao;

	/**
	 * Provide default constructor with low visibility in case user wants to use
	 * use aop:proxy-target-class="true" for transaction interceptor.
	 */
	SimpleJobRepository() {
	}

	public SimpleJobRepository(JobInstanceDao jobInstanceDao, JobExecutionDao jobExecutionDao,
			StepExecutionDao stepExecutionDao) {
		super();
		this.jobInstanceDao = jobInstanceDao;
		this.jobExecutionDao = jobExecutionDao;
		this.stepExecutionDao = stepExecutionDao;
	}

	/**
	 * <p>
	 * Create a {@link JobExecution} based on the passed in {@link Job} and
	 * {@link JobParameters}. However, unique identification of a job can only
	 * come from the database, and therefore must come from JobDao by either
	 * creating a new job instance or finding an existing one, which will ensure
	 * that the id of the job instance is populated with the correct value.
	 * </p>
	 * 
	 * <p>
	 * There are two ways in which the method determines if a job should be
	 * created or an existing one should be returned. The first is
	 * restartability. The {@link JobSupport} restartable property will be
	 * checked first. If it is false, a new job will be created, regardless of
	 * whether or not one exists. If it is true, the {@link JobInstanceDao} will
	 * be checked to determine if the job already exists, if it does, it's steps
	 * will be populated (there must be at least 1) and a new
	 * {@link JobExecution} will be returned. If no job instance is found, a new
	 * one will be created.
	 * </p>
	 * 
	 * <p>
	 * A check is made to see if any job executions are already running, and an
	 * exception will be thrown if one is detected. To detect a running job
	 * execution we use the {@link JobExecutionDao}:
	 * <ol>
	 * <li>First we find all jobs which match the given {@link JobParameters}
	 * and job name</li>
	 * <li>What happens then depends on how many existing job instances we
	 * find:
	 * <ul>
	 * <li>If there are none, or the {@link JobSupport} is marked restartable,
	 * then we create a new {@link JobInstance}</li>
	 * <li>If there is more than one and the {@link JobSupport} is not marked
	 * as restartable, it is an error. This could be caused by a job whose
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
	 * the same {@link JobParameters} and job name). The first transaction to
	 * complete in this scenario obtains a valid {@link JobExecution}, and
	 * others throw {@link JobExecutionAlreadyRunningException} (or timeout).
	 * There are no such guarantees if the {@link JobDao} does not respect the
	 * transaction isolation levels (e.g. if using a non-relational data-store,
	 * or if the platform does not support the higher isolation levels).
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

		List jobInstances = new ArrayList();
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

			jobInstances = jobInstanceDao.findJobInstances(job.getName(), jobParameters);
		}

		if (jobInstances.size() == 1) {
			// One job was found
			jobInstance = (JobInstance) jobInstances.get(0);
			jobInstance.setJobExecutionCount(jobExecutionDao.getJobExecutionCount(jobInstance));
			if (jobInstance.getJobExecutionCount() > job.getStartLimit()) {
				throw new BatchRestartException("Restart Max exceeded for Job: " + jobInstance.toString());
			}
			List executions = jobExecutionDao.findJobExecutions(jobInstance);
			JobExecution lastExecution = null;
			// check for running executions and find the last started
			for (Iterator iterator = executions.iterator(); iterator.hasNext();) {
				JobExecution execution = (JobExecution) iterator.next();
				if (lastExecution == null) {
					lastExecution = execution;
				}
				if (lastExecution.getStartTime().getTime() < execution.getStartTime().getTime()) {
					lastExecution = execution;
				}

				if (execution.isRunning()) {
					throw new JobExecutionAlreadyRunningException("A job execution for this job is already running: "
							+ jobInstance);
				}
			}
			jobInstance.setLastExecution(lastExecution);
			jobInstance.setJob(job);
		}
		else if (jobInstances.size() == 0) {
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

//	private List getStepNames(Job job) {
//		List stepNames = new ArrayList(job.getSteps().size());
//		for (Iterator iterator = job.getSteps().iterator(); iterator.hasNext();) {
//			Step step = (Step) iterator.next();
//			stepNames.add(step.getName());
//		}
//		return stepNames;
//	}
	


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
			jobExecutionDao.saveJobExecution(jobExecution);
		}
		else {
			// new execution
			jobExecutionDao.updateJobExecution(jobExecution);
		}
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
		Assert.notNull(stepExecution.getStepName(), "StepExecution's step name cannot be null.");

		if (stepExecution.getId() == null) {
			// new execution, obtain id and insert
			JobExecution jobExecution = stepExecution.getJobExecution();
			if (jobExecution.getId() == null) {
				jobExecutionDao.saveJobExecution(jobExecution);
			}
			stepExecutionDao.saveStepExecution(stepExecution);
			stepExecutionDao.saveExecutionContext(stepExecution);
		}
		else {
			// existing execution, update
			stepExecutionDao.updateStepExecution(stepExecution);
			stepExecutionDao.updateExecutionContext(stepExecution);
		}
	}

	/**
	 * Convenience method for creating a new job. A new job is created by
	 * calling {@link JobInstanceDao#createJobInstance(String, JobParameters)} and then it's
	 * list of StepInstances is passed to the createStepInstances method.
	 */
	private JobInstance createJobInstance(Job job, JobParameters jobParameters) {

		JobInstance jobInstance = jobInstanceDao.createJobInstance(job.getName(), jobParameters);
		jobInstance.setJob(job);
		return jobInstance;
	}

	public StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		List jobExecutions = jobExecutionDao.findJobExecutions(jobInstance);
		List stepExecutions = new ArrayList(jobExecutions.size());
		for (Iterator iterator = jobExecutions.iterator(); iterator.hasNext();) {
			JobExecution jobExecution = (JobExecution) iterator.next();
			StepExecution stepExecution = stepExecutionDao.getStepExecution(jobExecution, stepName);
			if (stepExecution != null) {
				stepExecutions.add(stepExecution);
			}
		}
		StepExecution latest = null;
		for (Iterator iterator = stepExecutions.iterator(); iterator.hasNext();) {
			StepExecution stepExecution = (StepExecution) iterator.next();
			if (latest == null) {
				latest = stepExecution;
			}
			if (latest.getStartTime().getTime() < stepExecution.getStartTime().getTime()) {
				latest = stepExecution;
			}
		}
		return latest;
	}

	public int getStepExecutionCount(JobInstance jobInstance, String stepName) {
		int count = 0;
		List jobExecutions = jobExecutionDao.findJobExecutions(jobInstance);
		for (Iterator iterator = jobExecutions.iterator(); iterator.hasNext();) {
			JobExecution jobExecution = (JobExecution) iterator.next();
			if (stepExecutionDao.getStepExecution(jobExecution, stepName) != null) {
				count++;
			}
		}
		return count;
	}

}
