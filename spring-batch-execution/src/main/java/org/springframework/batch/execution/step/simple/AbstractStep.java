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
package org.springframework.batch.execution.step.simple;

import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.domain.StepInterruptedException;
import org.springframework.batch.core.domain.StepSupport;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.io.exception.BatchCriticalException;
import org.springframework.batch.item.stream.SimpleStreamManager;
import org.springframework.batch.item.stream.StreamManager;
import org.springframework.batch.repeat.exception.handler.ExceptionHandler;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * A {@link Step} implementation that provides common behaviour to subclasses.
 * 
 * @author Dave Syer
 * 
 */
public abstract class AbstractStep extends StepSupport {

	private int skipLimit = 0;

	private ExceptionHandler exceptionHandler;

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	private Tasklet tasklet;

	private StreamManager streamManager;

	/**
	 * Default constructor.
	 */
	public AbstractStep() {
		super();
	}

	/**
	 * Convenient constructor for setting only the name property.
	 * @param name
	 */
	public AbstractStep(String name) {
		super(name);
	}

	public ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	public int getSkipLimit() {
		return skipLimit;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Public setter for the {@link StreamManager}. Set either this or the
	 * transaction manager, but not both.
	 * @param streamManager the {@link StreamManager} to set.
	 */
	public void setStreamManager(StreamManager streamManager) {
		this.streamManager = streamManager;
	}

	/**
	 * Assert that all mandatory properties are set (the {@link JobRepository}).
	 * 
	 * @throws Exception
	 */
	public void afterPropertiesSet() throws Exception {
		assertMandatoryProperties();
	}

	protected void assertMandatoryProperties() {
		Assert.notNull(jobRepository, "JobRepository is mandatory");
		Assert.state(transactionManager != null || streamManager != null,
				"Either StreamManager or TransactionManager must be set");
		Assert.state(transactionManager == null || streamManager == null,
				"Only one of StreamManager or TransactionManager must be set");
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.core.domain.StepSupport#process(org.springframework.batch.core.domain.StepExecution)
	 */
	public void execute(StepExecution stepExecution) throws StepInterruptedException, BatchCriticalException {
		SimpleStepExecutor executor = createStepExecutor();
		executor.execute(stepExecution);
	}

	/**
	 * Public setter for the tasklet.
	 * 
	 * @param tasklet the tasklet to set
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	/**
	 * @return a {@link SimpleStepExecutor} that can be used to launch the job.
	 */
	protected SimpleStepExecutor createStepExecutor() {
		assertMandatoryProperties();
		// Do not set the streamManager field if it is null, otherwise
		// the mandatory properties check will fail.
		StreamManager manager = streamManager;
		if (streamManager == null) {
			manager = new SimpleStreamManager(transactionManager);
		}
		SimpleStepExecutor executor = new SimpleStepExecutor(manager, this);
		executor.setRepository(jobRepository);
		executor.applyConfiguration(this);
		executor.setTasklet(tasklet);
		return executor;
	}

}