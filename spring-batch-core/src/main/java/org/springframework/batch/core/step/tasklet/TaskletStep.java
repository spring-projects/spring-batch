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
package org.springframework.batch.core.step.tasklet;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.listener.CompositeStepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link Step} that executes a {@link Tasklet} directly. This step does not
 * manage transactions or any looping functionality. The tasklet should do this
 * on its own.
 * 
 * @author Ben Hale
 */
public class TaskletStep extends AbstractStep implements Step, InitializingBean, BeanNameAware {

	private static final Log logger = LogFactory.getLog(TaskletStep.class);

	private Tasklet tasklet;

	private JobRepository jobRepository;

	private CompositeStepExecutionListener listener = new CompositeStepExecutionListener();

	/**
	 * Set the name property if it is not already set. Because of the order of
	 * the callbacks in a Spring container the name property will be set first
	 * if it is present. Care is needed with bean definition inheritance - if a
	 * parent bean has a name, then its children need an explicit name as well,
	 * otherwise they will not be unique.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	/**
	 * Register each of the objects as listeners. If the {@link Tasklet} itself
	 * implements this interface it will be registered automatically, but its
	 * injected dependencies will not be. This is a good way to get access to
	 * job parameters and execution context if the tasklet is parameterised.
	 * 
	 * @param listeners an array of listener objects of known types.
	 */
	public void setStepListeners(StepExecutionListener[] listeners) {
		for (int i = 0; i < listeners.length; i++) {
			this.listener.register(listeners[i]);
		}
	}

	/**
	 * Check mandatory properties.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobRepository, "JobRepository is mandatory for TaskletStep");
		Assert.notNull(tasklet, "Tasklet is mandatory for TaskletStep");
		if (tasklet instanceof StepExecutionListener) {
			listener.register((StepExecutionListener) tasklet);
		}
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

	public void execute(StepExecution stepExecution) throws JobInterruptedException, UnexpectedJobExecutionException {
		stepExecution.setStartTime(new Date());
		updateStatus(stepExecution, BatchStatus.STARTED);

		ExitStatus exitStatus = ExitStatus.FAILED;
		Exception fatalException = null;
		try {

			listener.beforeStep(stepExecution);
			exitStatus = tasklet.execute();
			exitStatus = exitStatus.and(listener.afterStep(stepExecution));

			try {
				jobRepository.saveOrUpdateExecutionContext(stepExecution);
				updateStatus(stepExecution, BatchStatus.COMPLETED);
			}
			catch (Exception e) {
				fatalException = e;
				updateStatus(stepExecution, BatchStatus.UNKNOWN);
			}

		}
		catch (Exception e) {
			logger.error("Encountered an error running the tasklet");
			updateStatus(stepExecution, BatchStatus.FAILED);
			try {
				exitStatus = exitStatus.and(listener.onErrorInStep(stepExecution, e));
			}
			catch (Exception ex) {
				logger.error("Encountered an error on listener close.", ex);
			}
			if (e instanceof JobInterruptedException) {
				throw (JobInterruptedException) e;
			}
			else if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new UnexpectedJobExecutionException(e);
		}
		finally {
			stepExecution.setExitStatus(exitStatus);
			stepExecution.setEndTime(new Date());
			try {
				jobRepository.saveOrUpdate(stepExecution);
			}
			catch (Exception e) {
				fatalException = e;
			}
			if (fatalException != null) {
				logger.error("Encountered an error saving batch meta data."
						+ "This job is now in an unknown state and should not be restarted.", fatalException);
				throw new UnexpectedJobExecutionException("Encountered an error saving batch meta data.", fatalException);
			}
		}

	}

	private void updateStatus(StepExecution stepExecution, BatchStatus status) {
		stepExecution.setStatus(status);
	}

}
