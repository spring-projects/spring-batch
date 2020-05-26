/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.core;

import org.springframework.util.Assert;

/**
 * Batch domain object representing a uniquely identifiable job run.
 * JobInstance can be restarted multiple times in case of execution failure and
 * it's lifecycle ends with first successful execution.
 *
 * Trying to execute an existing JobInstance that has already completed
 * successfully will result in error. Error will be raised also for an attempt
 * to restart a failed JobInstance if the Job is not restartable.
 *
 * @see Job
 * @see JobParameters
 * @see JobExecution
 * @see javax.batch.runtime.JobInstance
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
@SuppressWarnings("serial")
public class JobInstance extends Entity implements javax.batch.runtime.JobInstance{

	private final String jobName;

	public JobInstance(Long id, String jobName) {
		super(id);
		Assert.hasLength(jobName, "A jobName is required");
		this.jobName = jobName;
	}

	/**
	 * @return the job name. (Equivalent to getJob().getName())
	 */
	@Override
	public String getJobName() {
		return jobName;
	}

	@Override
	public String toString() {
		return super.toString() + ", Job=[" + jobName + "]";
	}

	@Override
	public long getInstanceId() {
		return super.getId();
	}
}
