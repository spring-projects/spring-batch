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

package org.springframework.batch.core.domain;

import java.sql.Timestamp;

/**
 * Batch domain object representing the execution of a job.  
 * 
 * @author Lucas Ward
 *
 */
public class JobExecution extends Entity {

//	TODO declare transient or make serializable
	private BatchStatus status = BatchStatus.STARTING;

	private Timestamp startTime = new Timestamp(System.currentTimeMillis());

	private Timestamp endTime = null;

	private Long jobId;

	private int exitCode;
	
	// Package private constructor for Hibernate
	JobExecution() {}

	/**
	 * Because a JobExecution isn't valid unless the jobId is set, this
	 * constructor is the only valid one.
	 * 
	 * @param jobId
	 */
	public JobExecution(Long jobId) {
		this.jobId = jobId;
	}

	public Timestamp getEndTime() {
		return endTime;
	}

	public void setEndTime(Timestamp endTime) {
		this.endTime = endTime;
	}

	public Timestamp getStartTime() {
		return startTime;
	}

	public void setStartTime(Timestamp startTime) {
		this.startTime = startTime;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public void setStatus(BatchStatus status) {
		this.status = status;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	/**
	 * @param exitCode
	 */
	public void setExitCode(int exitCode) {
		this.exitCode = exitCode;
	}
	
	/**
	 * @return the exitCode
	 */
	public int getExitCode() {
		return exitCode;
	}
}
