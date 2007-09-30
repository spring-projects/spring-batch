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

package org.springframework.batch.execution.job;

import java.sql.Timestamp;
import java.util.Iterator;
import java.util.List;

import org.springframework.batch.core.configuration.JobConfiguration;
import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInstance;
import org.springframework.batch.core.executor.ExitCodeExceptionClassifier;
import org.springframework.batch.core.executor.JobExecutor;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.core.executor.StepExecutorFactory;
import org.springframework.batch.core.executor.StepInterruptedException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.execution.step.SimpleStepExecutorFactory;
import org.springframework.batch.execution.step.simple.SimpleExitCodeExceptionClassifier;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Default implementation of (@link JobExecutor} interface. Sequentially executes a
 * job by iterating it's life of steps. Interruption of a job run is pluggable
 * by passing in various interruption policies.
 * 
 * @author Lucas Ward
 */
public class DefaultJobExecutor implements JobExecutor {

	private static final SimpleStepExecutorFactory DEFAULT_STEP_EXECUTOR_FACTORY = new SimpleStepExecutorFactory();

	private JobRepository jobRepository;

	private StepExecutorFactory stepExecutorFactory = DEFAULT_STEP_EXECUTOR_FACTORY;
	
	private ExitCodeExceptionClassifier exceptionClassifier = new SimpleExitCodeExceptionClassifier();

	public ExitStatus run(JobConfiguration configuration, JobExecution jobExecution)
			throws BatchCriticalException {

		JobInstance job = jobExecution.getJob();
		updateStatus(jobExecution, BatchStatus.STARTING);

		List steps = job.getSteps();

		ExitStatus status = ExitStatus.FAILED;

		try {
			for (Iterator i = steps.iterator(), j = configuration.getStepConfigurations().iterator(); i.hasNext()
					&& j.hasNext();) {
				StepInstance step = (StepInstance) i.next();
				StepConfiguration stepConfiguration = (StepConfiguration) j.next();
				if (shouldStart(step, stepConfiguration)) {
					updateStatus(jobExecution, BatchStatus.STARTED);
					StepExecutor stepExecutor = stepExecutorFactory.getExecutor(stepConfiguration);
					StepExecution stepExecution = new StepExecution(step, jobExecution);
					status = stepExecutor.process(stepConfiguration, stepExecution);
				}
			}

			updateStatus(jobExecution, BatchStatus.COMPLETED);
		}
		catch (StepInterruptedException e) {
			updateStatus(jobExecution, BatchStatus.STOPPED);
			status = exceptionClassifier.classifyForExitCode(e);
			rethrow(e);
		}
		catch (Throwable t) {
			updateStatus(jobExecution, BatchStatus.FAILED);
			status = exceptionClassifier.classifyForExitCode(t);
			rethrow(t);
		}
		finally {
			jobExecution.setEndTime(new Timestamp(System.currentTimeMillis()));
			jobExecution.setExitCode(status.getExitCode());
			jobRepository.saveOrUpdate(jobExecution);
		}
		
		return status;
	}

	private void updateStatus(JobExecution jobExecution, BatchStatus status) {
		JobInstance job = jobExecution.getJob();
		jobExecution.setStatus(status);
		job.setStatus(status);
		jobRepository.update(job);
		jobRepository.saveOrUpdate(jobExecution);
		for (Iterator iter = jobExecution.getStepContexts().iterator(); iter.hasNext();) {
			RepeatContext context = (RepeatContext) iter.next();
			context.setAttribute("JOB_STATUS", status);
		}
	}

	/*
	 * Given a step and configuration, return true if the step should start,
	 * false if it should not, and throw an exception if the job should finish.
	 */
	private boolean shouldStart(StepInstance step, StepConfiguration stepConfiguration) {

		if (step.getStatus() == BatchStatus.COMPLETED && stepConfiguration.isAllowStartIfComplete() == false) {
			// step is complete, false should be returned, indicated that the
			// step should
			// not be started
			return false;
		}

		if (step.getStepExecutionCount() < stepConfiguration.getStartLimit()) {
			// step start count is less than start max, return true
			return true;
		}
		else {
			// start max has been exceeded, throw an exception.
			throw new BatchCriticalException("Maximum start limit exceeded for step: " + step.getName() + "StartMax: "
					+ stepConfiguration.getStartLimit());
		}
	}

	/**
	 * @param t
	 */
	private static void rethrow(Throwable t) throws RuntimeException {
		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		else {
			throw new BatchCriticalException(t);
		}
	}

	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
		DEFAULT_STEP_EXECUTOR_FACTORY.setJobRepository(jobRepository);
	}

	public void setStepExecutorFactory(StepExecutorFactory stepExecutorResolver) {
		this.stepExecutorFactory = stepExecutorResolver;
	}

	public void setExceptionClassifier(
			ExitCodeExceptionClassifier exceptionClassifier) {
		this.exceptionClassifier = exceptionClassifier;
	}
}
