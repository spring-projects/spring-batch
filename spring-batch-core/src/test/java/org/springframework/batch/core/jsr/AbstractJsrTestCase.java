/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.jsr;

import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;

/**
 * @author mminella
 */
public abstract class AbstractJsrTestCase {

	protected static JobOperator operator;

	static {
		operator = BatchRuntime.getJobOperator();
	}

	/**
	 * Executes a job and waits for it's status to be any of {@link javax.batch.runtime.BatchStatus#STOPPED},
	 * {@link javax.batch.runtime.BatchStatus#COMPLETED}, or {@link javax.batch.runtime.BatchStatus#FAILED}.  If the job does not
	 * reach one of those statuses within the given timeout, a {@link java.util.concurrent.TimeoutException} is
	 * thrown.
	 *
	 * @param jobName Name of the job to run
	 * @param properties Properties to pass the job
	 * @param timeout length of time to wait for a job to finish
	 * @return the {@link javax.batch.runtime.JobExecution} for the final state of the job
	 * @throws java.util.concurrent.TimeoutException if the timeout occurs
	 */
	public static JobExecution runJob(String jobName, Properties properties, long timeout) throws TimeoutException {
		System.out.println("Operator = " + operator);
		long executionId = operator.start(jobName, properties);
		JobExecution execution = operator.getJobExecution(executionId);

		Date curDate = new Date();
		BatchStatus curBatchStatus = execution.getBatchStatus();

		while(true) {
			if(curBatchStatus == BatchStatus.STOPPED || curBatchStatus == BatchStatus.COMPLETED || curBatchStatus == BatchStatus.FAILED) {
				break;
			}

			if(new Date().getTime() - curDate.getTime() > timeout) {
				throw new TimeoutException("Job processing did not complete in time");
			}

			execution = operator.getJobExecution(executionId);
			curBatchStatus = execution.getBatchStatus();
		}
		return execution;
	}

	/**
	 * Restarts a job and waits for it's status to be any of {@link BatchStatus#STOPPED},
	 * {@link BatchStatus#COMPLETED}, or {@link BatchStatus#FAILED}.  If the job does not
	 * reach one of those statuses within the given timeout, a {@link java.util.concurrent.TimeoutException} is
	 * thrown.
	 *
	 * @param executionId The execution id to restart
	 * @param properties The Properties to pass to the new run
	 * @param timeout The length of time to wait for the job to run
	 * @return the {@link JobExecution} for the final state of the job
	 * @throws java.util.concurrent.TimeoutException if the timeout occurs
	 */
	public static JobExecution restartJob(long executionId, Properties properties, long timeout) throws TimeoutException {
		long restartId = operator.restart(executionId, properties);
		JobExecution execution = operator.getJobExecution(restartId);

		Date curDate = new Date();
		BatchStatus curBatchStatus = execution.getBatchStatus();

		while(true) {
			if(curBatchStatus == BatchStatus.STOPPED || curBatchStatus == BatchStatus.COMPLETED || curBatchStatus == BatchStatus.FAILED) {
				break;
			}

			if(new Date().getTime() - curDate.getTime() > timeout) {
				throw new TimeoutException("Job processing did not complete in time");
			}

			execution = operator.getJobExecution(restartId);
			curBatchStatus = execution.getBatchStatus();
		}
		return execution;
	}

	public static Metric getMetric(StepExecution stepExecution, Metric.MetricType type) {
		Metric[] metrics = stepExecution.getMetrics();

		for (Metric metric : metrics) {
			if(metric.getType() == type) {
				return metric;
			}
		}

		return null;
	}


}
