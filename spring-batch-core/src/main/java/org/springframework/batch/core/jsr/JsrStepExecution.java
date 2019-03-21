/*
 * Copyright 2013 the original author or authors.
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

import java.io.Serializable;
import java.util.Date;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Implementation of the JsrStepExecution as defined in JSR-352.  This implementation
 * wraps a {@link org.springframework.batch.core.StepExecution} as it's source of
 * data.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrStepExecution implements javax.batch.runtime.StepExecution{

	private final static String PERSISTENT_USER_DATA_KEY = "batch_jsr_persistentUserData";
	private final org.springframework.batch.core.StepExecution stepExecution;
	// The API for the persistent user data is handled by the JsrStepContext which is why the name here is based on the JsrStepContext.
	private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport(ClassUtils.getShortName(JsrStepContext.class));

	/**
	 * @param stepExecution The {@link org.springframework.batch.core.StepExecution} used
	 * as the basis for the data.
	 */
	public JsrStepExecution(org.springframework.batch.core.StepExecution stepExecution) {
		Assert.notNull(stepExecution, "A StepExecution is required");

		this.stepExecution = stepExecution;
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getStepExecutionId()
	 */
	@Override
	public long getStepExecutionId() {
		return stepExecution.getId();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getStepName()
	 */
	@Override
	public String getStepName() {
		return stepExecution.getStepName();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getBatchStatus()
	 */
	@Override
	public BatchStatus getBatchStatus() {
		return stepExecution.getStatus().getBatchStatus();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getStartTime()
	 */
	@Override
	public Date getStartTime() {
		return stepExecution.getStartTime();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getEndTime()
	 */
	@Override
	public Date getEndTime() {
		return stepExecution.getEndTime();
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getExitStatus()
	 */
	@Override
	public String getExitStatus() {
		ExitStatus status = stepExecution.getExitStatus();

		if(status == null) {
			return null;
		} else {
			return status.getExitCode();
		}
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getPersistentUserData()
	 */
	@Override
	public Serializable getPersistentUserData() {
		return (Serializable) stepExecution.getExecutionContext().get(executionContextUserSupport.getKey(PERSISTENT_USER_DATA_KEY));
	}

	/* (non-Javadoc)
	 * @see javax.batch.runtime.JsrStepExecution#getMetrics()
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
