/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.job;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.batch.core.Entity;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.util.Assert;

/**
 * Batch domain object representing a uniquely identifiable job run. {@code JobInstance}
 * can be restarted multiple times in case of execution failure, and its lifecycle ends
 * with first successful execution.
 * <p>
 * Trying to execute an existing {@code JobInstance} that has already completed
 * successfully results in an error. An error is also raised for an attempt to restart a
 * failed {@code JobInstance} if the {@code Job} is not restartable.
 *
 * @see Job
 * @see JobParameters
 * @see JobExecution
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public class JobInstance extends Entity {

	private final String jobName;

	private final List<JobExecution> jobExecutions = Collections.synchronizedList(new LinkedList<>());

	/**
	 * Constructor for {@link JobInstance}.
	 * @param id The instance ID.
	 * @param jobName The name associated with the {@link JobInstance}.
	 */
	public JobInstance(long id, String jobName) {
		super(id);
		Assert.hasLength(jobName, "A jobName is required");
		this.jobName = jobName;
	}

	/**
	 * @return the job name. (Equivalent to {@code getJob().getName()}).
	 */
	public String getJobName() {
		return this.jobName;
	}

	/**
	 * Returns an immutable copy of the list of {@link JobExecution}s associated with this
	 * JobInstance.
	 * @return the job executions
	 */
	public List<JobExecution> getJobExecutions() {
		return List.copyOf(this.jobExecutions);
	}

	/**
	 * Adds the job name to the string representation of the super class ({@link Entity}).
	 */
	@Override
	public String toString() {
		return super.toString() + ", Job=[" + this.jobName + "]";
	}

	/**
	 * @return The current instance ID.
	 * @deprecated in favor of {@link #getId()}
	 */
	@Deprecated(forRemoval = true)
	public long getInstanceId() {
		return super.getId();
	}

	public void addJobExecution(JobExecution jobExecution) {
		this.jobExecutions.add(jobExecution);
	}

}
