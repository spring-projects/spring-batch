package org.springframework.batch.sample.advice;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.domain.BatchStatus;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobInterruptedException;

/**
 * Monitors {@link JobExecution} and throws a {@link JobInterruptedException} in
 * case the execution has been requested to stop using
 * {@link JobExecution#stop()} or executing thread has been interrupted.
 * 
 * @author Robert Kasanicky
 */
public class DefaultJobInterruptionAdvice implements JobInterruptionAdvice {

	private static final Log logger = LogFactory.getLog(DefaultJobInterruptionAdvice.class);

	private JobExecution jobExecution;

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.core.runtime.JobInterruptionAdvice#checkInterrupt()
	 */
	public void checkInterrupt() throws JobInterruptedException {

		logger.info("checking job execution for interrupt");

		if (Thread.currentThread().isInterrupted() || jobExecution.getStatus() == BatchStatus.STOPPING) {
			throw new JobInterruptedException("Job execution interrupted by user");
		}
	}

	/**
	 * Setter for JobExecution - to be applied as 'after returning' advice that
	 * captures return value on the method that creates {@link JobExecution} for
	 * the job run.
	 */
	public void setJobExecution(JobExecution jobExecution) {
		this.jobExecution = jobExecution;
		logger.info("JobExecution set");
	}
}
