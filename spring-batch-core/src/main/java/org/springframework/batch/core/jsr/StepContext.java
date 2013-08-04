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
import java.util.Properties;

import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.Metric;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.util.Assert;

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

	@Override
	public String getStepName() {
		return stepExecution.getStepName();
	}

	@Override
	public Object getTransientUserData() {
		return transientUserData;
	}

	@Override
	public void setTransientUserData(Object data) {
		this.transientUserData = data;
	}

	@Override
	public long getStepExecutionId() {
		return stepExecution.getId();
	}

	@Override
	public Properties getProperties() {
		return jobParametersConveter.getProperties(this.stepExecution.getJobParameters());
	}

	@Override
	public Serializable getPersistentUserData() {
		return null;
	}

	@Override
	public void setPersistentUserData(Serializable data) {
	}

	@Override
	public BatchStatus getBatchStatus() {
		return stepExecution.getStatus().getBatchStatus();
	}

	@Override
	public String getExitStatus() {
		return stepExecution.getExitStatus().getExitCode();
	}

	@Override
	public void setExitStatus(String status) {
		stepExecution.setExitStatus(new ExitStatus(status));
	}

	@Override
	public Exception getException() {
		return null;
	}

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
