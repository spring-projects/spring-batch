/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.batch.test;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.Metric;
import javax.batch.runtime.StepExecution;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.springframework.lang.Nullable;

/**
 * Provides testing utilities to execute JSR-352 jobs and block until they are complete (since all JSR-352 based jobs
 * are executed asynchronously).
 *
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class JsrTestUtils {

	private static JobOperator operator;

	static {
		operator = BatchRuntime.getJobOperator();
	}

	private JsrTestUtils() {}

	/**
	 * Executes a job and waits for it's status to be any of {@link BatchStatus#STOPPED},
	 * {@link BatchStatus#COMPLETED}, or {@link BatchStatus#FAILED}.  If the job does not
	 * reach one of those statuses within the given timeout, a {@link java.util.concurrent.TimeoutException} is
	 * thrown.
	 *
	 * @param jobName the name of the job.
	 * @param properties job parameters to be associated with the job.
	 * @param timeout maximum amount of time to wait in milliseconds.
	 * @return the {@link JobExecution} for the final state of the job
	 * @throws java.util.concurrent.TimeoutException if the timeout occurs
	 */
	public static JobExecution runJob(String jobName, Properties properties, long timeout) throws TimeoutException{
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
	 * @param executionId the id of the job execution to restart.
	 * @param properties job parameters to be associated with the job.
	 * @param timeout maximum amount of time to wait in milliseconds.
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

	@Nullable
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
