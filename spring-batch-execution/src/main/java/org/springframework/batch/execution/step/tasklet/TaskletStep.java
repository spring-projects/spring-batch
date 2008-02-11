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
package org.springframework.batch.execution.step.tasklet;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link Step} that executes a {@link Tasklet} directly. This step does not
 * manage transactions or any looping functionality. The tasklet should do this
 * on its own.
 * 
 * @author Ben Hale
 */
public class TaskletStep extends StepSupport implements InitializingBean {

	private static final Log logger = LogFactory.getLog(TaskletStep.class);

	private Tasklet tasklet;

	private JobRepository jobRepository;

	private RepeatListener[] listeners = new RepeatListener[] {};
	
	public void setListeners(RepeatListener[] listeners) {
		this.listeners  = listeners;
	}

	public void setListener(RepeatListener listener) {
		listeners = new RepeatListener[] { listener };
	}	

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository, "JobRepository is mandatory for TaskletStep");
		Assert.notNull(tasklet, "Tasklet is mandatory for TaskletStep");
	}

	/**
	 * Default constructor is useful for XML configuration.
	 */
	public TaskletStep() {
		super();
	}

	/**
	 * Creates a new <code>Step</code> for executing a <code>Tasklet</code>
	 * 
	 * @param tasklet The <code>Tasklet</code> to execute
	 * @param jobRepository The <code>JobRepository</code> to use for
	 * persistence of incremental state
	 */
	public TaskletStep(Tasklet tasklet, JobRepository jobRepository) {
		this();
		this.tasklet = tasklet;
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link Tasklet}.
	 * @param tasklet the {@link Tasklet} to set
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	/**
	 * Public setter for the {@link JobRepository}.
	 * @param jobRepository the {@link JobRepository} to set
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	public void execute(StepExecution stepExecution) throws JobInterruptedException, BatchCriticalException {
		stepExecution.setStartTime(new Date());
		updateStatus(stepExecution, BatchStatus.STARTED);

		ExitStatus exitStatus = ExitStatus.FAILED;
		try {

			// We are using the RepeatTemplate as a vehicle for the listener
			// so it can be set up cheaply here with standard properties.
			RepeatTemplate template = new RepeatTemplate();
			template.setCompletionPolicy(new SimpleCompletionPolicy(1));

			template.setListeners(listeners);
			exitStatus =template.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					return tasklet.execute();
				}	
			});
			
			updateStatus(stepExecution, BatchStatus.COMPLETED);
			
		}
		catch (Exception e) {
			logger.error("Encountered an error running the tasklet");
			updateStatus(stepExecution, BatchStatus.FAILED);
			throw new BatchCriticalException(e);
		}
		finally {
			stepExecution.setExitStatus(exitStatus);
			stepExecution.setEndTime(new Date());
			jobRepository.saveOrUpdate(stepExecution);
		}
	}

	private void updateStatus(StepExecution stepExecution, BatchStatus status) {
		stepExecution.setStatus(status);
		jobRepository.saveOrUpdate(stepExecution);
	}

}
