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
package org.springframework.batch.core.jsr;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.util.Assert;

/**
 * Wrapper class to provide the {@link javax.batch.runtime.context.StepContext} functionality
 * as specified in JSR-352.  Wrapper delegates to the underlying {@link StepExecution} to
 * obtain the related contextual information.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class StepContext implements javax.batch.runtime.context.StepContext {

	private StepExecution stepExecution;
	private Object transientUserData;
	private JobParametersConverter jobParametersConveter;

	public StepContext(StepExecution stepExecution, JobParametersConverter jobParametersConveter) {
		Assert.notNull(stepExecution, "A StepExecution is required");
		Assert.notNull(jobParametersConveter, "A ParametersConverter is required");

		this.stepExecution = stepExecution;
		this.jobParametersConveter = jobParametersConveter;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getStepName()
	 */
	@Override
	public String getStepName() {
		return stepExecution.getStepName();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getTransientUserData()
	 */
	@Override
	public Object getTransientUserData() {
		return transientUserData;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#setTransientUserData(java.lang.Object)
	 */
	@Override
	public void setTransientUserData(Object data) {
		this.transientUserData = data;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getStepExecutionId()
	 */
	@Override
	public long getStepExecutionId() {
		return stepExecution.getId();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getProperties()
	 */
	@Override
	public Properties getProperties() {
		//TODO: Fix this...this should be properties, not parameters.  Waiting on BATCH-2001
		return jobParametersConveter.getProperties(this.stepExecution.getJobParameters());
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getPersistentUserData()
	 */
	@Override
	public Serializable getPersistentUserData() {
		return (Serializable) stepExecution.getExecutionContext().get("batch_jsr_persistentUserData");
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#setPersistentUserData(java.io.Serializable)
	 */
	@Override
	public void setPersistentUserData(Serializable data) {
		stepExecution.getExecutionContext().put("batch_jsr_persistentUserData", data);
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getBatchStatus()
	 */
	@Override
	public BatchStatus getBatchStatus() {
		return stepExecution.getStatus().getBatchStatus();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getExitStatus()
	 */
	@Override
	public String getExitStatus() {
		return stepExecution.getExitStatus().getExitCode();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#setExitStatus(java.lang.String)
	 */
	@Override
	public void setExitStatus(String status) {
		stepExecution.setExitStatus(new ExitStatus(status));
	}

	/**
	 * To support both JSR-352's requirement to return the most recent exception
	 * and Spring Batch's support for {@link Throwable}, this implementation will
	 * return the most recent exception in the underlying {@link StepExecution}'s
	 * failure exceptions list.  If the exception there extends {@link Throwable}
	 * instead of {@link Exception}, it will be wrapped in an {@link Exception} and
	 * then returned.
	 *
	 * @see javax.batch.runtime.context.StepContext#getException()
	 */
	@Override
	public Exception getException() {
		List<Throwable> failureExceptions = stepExecution.getFailureExceptions();
		if(failureExceptions == null || failureExceptions.isEmpty()) {
			return null;
		} else {
			Throwable t = failureExceptions.get(failureExceptions.size() - 1);

			if(t instanceof Exception) {
				return (Exception) t;
			} else {
				return new Exception(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.context.StepContext#getMetrics()
	 */
	@Override
	public Metric[] getMetrics() {
		Metric[] metrics = new Metric[8];

		metrics[0] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.COMMIT_COUNT, stepExecution.getCommitCount());
		metrics[1] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.FILTER_COUNT, stepExecution.getFilterCount());
		metrics[2] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.PROCESS_SKIP_COUNT, stepExecution.getProcessSkipCount());
		metrics[3] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.READ_COUNT, stepExecution.getReadCount());
		metrics[4] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.READ_SKIP_COUNT, stepExecution.getReadSkipCount());
		metrics[5] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.ROLLBACK_COUNT, stepExecution.getRollbackCount());
		metrics[6] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.WRITE_COUNT, stepExecution.getWriteCount());
		metrics[7] = new SimpleMetric(javax.batch.runtime.Metric.MetricType.WRITE_SKIP_COUNT, stepExecution.getWriteSkipCount());

		return metrics;
	}
}
