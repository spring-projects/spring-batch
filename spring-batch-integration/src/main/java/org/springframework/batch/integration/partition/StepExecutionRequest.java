package org.springframework.batch.integration.partition;

import java.io.Serializable;

public class StepExecutionRequest implements Serializable {

	private final Long stepExecutionId;

	private final String stepName;

	private final Long jobExecutionId;

	public StepExecutionRequest(String stepName, Long jobExecutionId, Long stepExecutionId) {
		this.stepName = stepName;
		this.jobExecutionId = jobExecutionId;
		this.stepExecutionId = stepExecutionId;
	}

	public Long getJobExecutionId() {
		return jobExecutionId;
	}

	public Long getStepExecutionId() {
		return stepExecutionId;
	}

	public String getStepName() {
		return stepName;
	}

	@Override
	public String toString() {
		return String.format("StepExecutionRequest: [jobExecutionId=%d, stepExecutionId=%d, stepName=%s]",
				jobExecutionId, stepExecutionId, stepName);
	}

}
