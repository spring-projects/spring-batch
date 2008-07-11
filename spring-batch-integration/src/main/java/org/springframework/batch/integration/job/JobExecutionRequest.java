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
package org.springframework.batch.integration.job;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;

/**
 * Encapsulation of a request to execute a job execution through a message flow
 * consisting of step handlers. A handler should pass the message on as it is,
 * modifying the request properties as necessary. Generally a handler will
 * execute a step as part of the {@link JobExecution} passed in, and should
 * change the status to {@link BatchStatus#COMPLETED} if the step is successful
 * (generally a handler cannot determine if the whole job execution is complete,
 * so this is just information about the step).<br/>
 * 
 * If the incoming status is {@link BatchStatus#FAILED},
 * {@link BatchStatus#STOPPED} or {@link BatchStatus#STOPPING} the request
 * should be ignored by handlers (passed on without modification).
 * 
 * @author Dave Syer
 * 
 */
public class JobExecutionRequest {

	private JobExecution jobExecution;

	private BatchStatus status;

	private Throwable throwable;

	/**
	 * @param jobExecution
	 */
	public JobExecutionRequest(JobExecution jobExecution) {
		this.jobExecution = jobExecution;
		status = jobExecution.getStatus();
	}

	/**
	 * @return the current job execution id
	 */
	public Long getJobId() {
		return this.jobExecution.getJobId();
	}

	/**
	 * @return the current {@link BatchStatus}
	 */
	public BatchStatus getStatus() {
		return status;
	}

	/**
	 * Public setter for the status.
	 * @param status the status to set
	 */
	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	/**
	 * @return true if there are errors
	 */
	public boolean hasErrors() {
		return throwable != null;
	}

	/**
	 * Public getter for the throwable.
	 * @return the throwable
	 */
	public Throwable getLastThrowable() {
		return throwable;
	}

	/**
	 * Public setter for the throwable.
	 * @param throwable the throwable to set
	 */
	public void registerThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	/**
	 * Public getter for the jobExecution.
	 * @return the jobExecution
	 */
	public JobExecution getJobExecution() {
		return jobExecution;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getClass().getSimpleName()+": "+jobExecution;
	}

}
