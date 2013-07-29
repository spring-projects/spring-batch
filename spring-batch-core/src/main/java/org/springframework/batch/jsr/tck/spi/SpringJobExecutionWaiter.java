/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.jsr.tck.spi;

import java.util.Date;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.springframework.util.Assert;

import com.ibm.jbatch.tck.spi.JobExecutionTimeoutException;
import com.ibm.jbatch.tck.spi.JobExecutionWaiter;

/**
 * A listener to provide the JSR-352 TCK to be notified once a job has been completed.
 * 
 * @author Michael Minella
 */
public class SpringJobExecutionWaiter implements JobExecutionWaiter {

	private JobOperator operator;
	private long sleepTime;
	private long executionId;
	private long timeout;

	/**
	 * Constructor requiring a reference to a {@link JobOperator}, interval of how often to check if the job
	 * is complete, a timeout and an id of the job execution.
	 * 
	 * @param operator - The {@link JobOperator}.  Required.
	 * @param sleepTime - How often to check the status of the requested job. Must be greater than 0.
	 * @param timeout - How long to let the job run.  If the value provided is less than zero, it will never timeout.
	 * @param executionId - The id of the {@link JobExecution} to check on.
	 */
	public SpringJobExecutionWaiter(JobOperator operator, long sleepTime, long timeout, long executionId) {
		Assert.notNull(operator);
		Assert.isTrue(sleepTime > 0);
		Assert.isTrue(executionId >= 0);

		this.operator = operator;
		this.sleepTime = sleepTime;
		this.executionId = executionId;
		this.timeout = timeout;
	}

	/* (non-Javadoc)
	 * @see com.ibm.jbatch.tck.spi.JobExecutionWaiter#awaitTermination()
	 */
	@Override
	public JobExecution awaitTermination() throws JobExecutionTimeoutException {

		JobExecution jobExecution = null;
		long startTime = new Date().getTime();

		while(true) {
			JobExecution curExecutionState = operator.getJobExecution(executionId);
			if(curExecutionState.getBatchStatus().compareTo(BatchStatus.STOPPED) >= 0) {
				jobExecution = curExecutionState;
				break;
			} else {
				long curTime = new Date().getTime();
				if(timeout > 0 && curTime - startTime > timeout) {
					throw new JobExecutionTimeoutException();
				} else {
					try {
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
						throw new RuntimeException("An error occured while waiting for the job to complete", e);
					}
				}
			}
		}

		return jobExecution;
	}
}
