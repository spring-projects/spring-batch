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

package org.springframework.batch.core.repository.dao;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;

import java.util.List;
import java.util.Map;

/**
 * Data Access Object for job parameters.
 *
 * @author Moad Elfatihi
 */
public interface JobParameterDao {

	/**
	 * Get the parameters associated with the given job execution id.
	 * @param executionId {@link Long} containing the id for the execution.
	 * @return job parameters for the requested execution id
	 * @since 6.1
	 */
	default JobParameters getJobParameters(Long executionId) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve the job parameters of several job executions at once, to avoid querying
	 * them one execution at a time.
	 * @param executionIds the ids of the job executions to retrieve the parameters of
	 * @return the parameters of each job execution that has any, keyed by execution id
	 * @since 6.1
	 */
	default Map<Long, JobParameters> getJobParameters(List<Long> executionIds) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Inserts all parameters from the provided JobParameters.
	 * @param executionId the id of a {@link JobExecution} for which job parameters should
	 * be inserted
	 * @since 6.1
	 */
	default void insertJobParameters(long executionId, JobParameters jobParameters) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete the parameters associated with the given job execution.
	 * @param jobExecution the job execution for which job parameters should be deleted
	 * @since 6.1
	 */
	default void deleteParametersOf(JobExecution jobExecution) {
		throw new UnsupportedOperationException();
	}

}
