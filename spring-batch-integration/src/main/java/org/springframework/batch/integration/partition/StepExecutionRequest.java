/*
 * Copyright 2009-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.integration.partition;

import java.io.Serializable;

/**
 * Class encapsulating information required to request a step execution in
 * a remote partitioning setup.
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class StepExecutionRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long stepExecutionId;

	private String stepName;

	private Long jobExecutionId;

	private StepExecutionRequest() {
		//For Jackson deserialization
	}

	/**
	 * Create a new {@link StepExecutionRequest} instance.
	 * @param stepName the name of the step to execute
	 * @param jobExecutionId the id of the job execution
	 * @param stepExecutionId the id of the step execution
	 */
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
