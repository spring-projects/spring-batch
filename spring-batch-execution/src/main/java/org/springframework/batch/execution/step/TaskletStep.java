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
package org.springframework.batch.execution.step;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.execution.scope.SimpleStepContext;
import org.springframework.batch.execution.scope.StepContext;
import org.springframework.batch.execution.scope.StepSynchronizationManager;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatListener;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
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
public class TaskletStep implements Step, InitializingBean, BeanNameAware {

	private static final Log logger = LogFactory.getLog(TaskletStep.class);

	private Tasklet tasklet;

	private JobRepository jobRepository;
	
	private String name;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete;

	public String getName() {
		return this.name;
	}

	/**
	 * Set the name property if it is not already set. Because of the order of the callbacks in a Spring container the
	 * name property will be set first if it is present. Care is needed with bean definition inheritance - if a parent
	 * bean has a name, then its children need an explicit name as well, otherwise they will not be unique.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	/**
	 * Set the name property. Always overrides the default value if this object is a Spring bean.
	 * 
	 * @see #setBeanName(java.lang.String)
	 */
	public void setName(String name) {
		this.name = name;
	}

	public int getStartLimit() {
		return this.startLimit;
	}

	/**
	 * Public setter for the startLimit.
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	public boolean isAllowStartIfComplete() {
		return this.allowStartIfComplete;
	}

	/**
	 * Public setter for the shouldAllowStartIfComplete.
	 * 
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}


	private RepeatListener[] listeners = new RepeatListener[] {};

	public void setListeners(RepeatListener[] listeners) {
		this.listeners = listeners;
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
		Exception fatalException = null;
		try {

			StepContext parentStepContext = StepSynchronizationManager.getContext();
			final StepContext stepContext = new SimpleStepContext(stepExecution, parentStepContext);
			StepSynchronizationManager.register(stepContext);

			// We are using the RepeatTemplate as a vehicle for the listener
			// so it can be set up cheaply here with standard properties.
			RepeatTemplate template = new RepeatTemplate();
			template.setCompletionPolicy(new SimpleCompletionPolicy(1));

			template.setListeners(listeners);
			exitStatus = template.iterate(new RepeatCallback() {
				public ExitStatus doInIteration(RepeatContext context) throws Exception {
					return tasklet.execute();
				}
			});

			try {
				jobRepository.saveOrUpdateExecutionContext(stepExecution);
				updateStatus(stepExecution, BatchStatus.COMPLETED);
			} catch (Exception e) {
				fatalException = e;
				updateStatus(stepExecution, BatchStatus.UNKNOWN);
			}

		}
		catch (RuntimeException e) {
			logger.error("Encountered an error running the tasklet");
			updateStatus(stepExecution, BatchStatus.FAILED);
			throw e;
		}
		catch (Exception e) {
			logger.error("Encountered an error running the tasklet");
			updateStatus(stepExecution, BatchStatus.FAILED);
			throw new BatchCriticalException(e);
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
			finally {
				StepSynchronizationManager.close();
				if (fatalException!=null) {
					logger.error("Encountered an error saving batch meta data."
							+ "This job is now in an unknown state and should not be restarted.", fatalException);
					throw new BatchCriticalException("Encountered an error saving batch meta data.", fatalException);
				}
			}
		}		
	
	}

	private void updateStatus(StepExecution stepExecution, BatchStatus status) {
		stepExecution.setStatus(status);
	}

}
