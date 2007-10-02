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
import java.util.Properties;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.repository.BatchRestartException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.execution.repository.dao.JobDao;
import org.springframework.batch.execution.repository.dao.StepDao;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
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

	public SimpleJobRepository(JobDao jobDao, StepDao stepDao) {
		super();
		this.jobDao = jobDao;
		this.stepDao = stepDao;
	}

	/**
	 * <p>
	 * Find or Create a JobInstance(@link JobInstance) based on the passed in
	 * RuntimeInformation and Configuration. JobRuntimeInformation contains the
	 * following fields which logically identify a job: JobName, JobStream,
	 * JobRun, and Schedule Date. However, unique identification of a job can
	 * only come from the database, and therefore must come from JobDao by
	 * either creating a new job or finding an existing one, which will ensure
	 * that the id field of the job is populated with the correct value.
	 * </p>
	 *
	 * <p>
	 * There are two ways in which the method determines if a job should be
	 * created or an existing one should be returned. The first is
	 * restartability. The Job's restartPolicy will be checked first. If it is
	 * not restartable, a new job will be created, regardless of whether or not
	 * one exists. If it is restartable, the JobDao will be checked to determine
	 * if the job already exists, if it does, it's steps will be populated
	 * (there must be at least 1) and it will be returned. If no job is found, a
	 * new one will be created based on the configuration.
	 * </p>
	 *
	 * @see JobRepository#findOrCreateJob(JobConfiguration, JobIdentifier)
	 * @throws BatchRestartException if more than one JobInstance if found
	 * 	or if JobInstance.getJobExecutionCount() is greater than JobConfiguration.getStartLimit()
	 */
	public JobInstance findOrCreateJob(JobConfiguration jobConfiguration, JobIdentifier runtimeInformation) {

		List jobs;

		// Check if a job is restartable, if not, create and return a new job
		if (jobConfiguration.isRestartable() == false) {
			return createJob(jobConfiguration, runtimeInformation);
		}
		else {
			// find all jobs matching the runtime information.
			jobs = jobDao.findJobs(runtimeInformation);
		}

		if (jobs.size() == 1) {
			// One job was found
			JobInstance job = (JobInstance) jobs.get(0);
			job.setSteps(findSteps(jobConfiguration.getStepConfigurations(), job));
			job.setJobExecutionCount(jobDao.getJobExecutionCount(job.getId()));
			if (job.getJobExecutionCount() > jobConfiguration.getStartLimit()) {
				throw new BatchRestartException("Restart Max exceeded for Job: " + job.toString());
			}
			return job;
		}
		else if (jobs.size() == 0) {
			// no job found, create one
			return createJob(jobConfiguration, runtimeInformation);
		}
		else {
			// More than one job found, throw exception
			throw new IncorrectResultSizeDataAccessException("Error restarting job, more than one JobInstance found for: "
					+ jobConfiguration.toString(), 1, jobs.size());
		}
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
	private JobInstance createJob(JobConfiguration jobConfiguration, JobIdentifier runtimeInformation) {

		JobInstance job = jobDao.createJob(runtimeInformation);
		job.setSteps(createSteps(job, jobConfiguration.getStepConfigurations()));
		return job;
	}

	/*
	 * Create steps based on the given Job and list of StepConfigurations.
	 */
	private List createSteps(JobInstance job, List stepConfigurations) {

		List steps = new ArrayList();
		Iterator i = stepConfigurations.iterator();
		while (i.hasNext()) {
			StepConfiguration stepConfiguration = (StepConfiguration) i.next();
			StepInstance step = stepDao.createStep(job, stepConfiguration.getName());
			//Ensure valid restart data is being returned.
			if(step.getRestartData() == null || step.getRestartData().getProperties() == null){
				step.setRestartData(new GenericRestartData(new Properties()));
			}
			steps.add(step);
		}

		return steps;
	}

	/*
	 * Find Steps for the given list of StepConfiguration's with a given JobId
	 */
	protected List findSteps(List stepConfigurations, JobInstance job) {
		List steps = new ArrayList();
		Iterator i = stepConfigurations.iterator();
		while (i.hasNext()) {

			StepConfiguration stepConfiguration = (StepConfiguration) i.next();
			StepInstance step = stepDao.findStep(job, stepConfiguration.getName());
			if (step != null) {

				step.setStepExecutionCount(stepDao.getStepExecutionCount(step.getId()));
				//Ensure valid restart data is being returned.
				if(step.getRestartData() == null || step.getRestartData().getProperties() == null){
					step.setRestartData(new GenericRestartData(new Properties()));
				}
				steps.add(step);
			}
		}
		return steps;
	}

}
