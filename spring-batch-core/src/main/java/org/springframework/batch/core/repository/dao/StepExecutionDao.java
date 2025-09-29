/*
 * Copyright 2006-2023 the original author or authors.
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

import java.util.Collections;
import java.util.List;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.lang.Nullable;

public interface StepExecutionDao {

	/**
	 * Create a new step execution with an assigned id. This method should not add the
	 * step execution to the job execution (no side effect on the parameter, this is done
	 * at the repository level).
	 * @param stepName the name of the step
	 * @param jobExecution the job execution the step execution belongs to
	 * @return a new {@link StepExecution} instance with an assigned id
	 * @since 6.0
	 */
	default StepExecution createStepExecution(String stepName, JobExecution jobExecution) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Update the given StepExecution
	 * <p>
	 * Preconditions: Id must not be null.
	 * @param stepExecution {@link StepExecution} instance to be updated.
	 */
	void updateStepExecution(StepExecution stepExecution);

	/**
	 * Retrieve a {@link StepExecution} from its id. The execution context will not be
	 * loaded. If you need the execution context, use the job repository which coordinates
	 * the calls to the various DAOs.
	 * @param stepExecutionId the step execution id
	 * @return a {@link StepExecution}
	 * @since 6.0
	 */
	@Nullable
	StepExecution getStepExecution(long stepExecutionId);

	/**
	 * Retrieve a {@link StepExecution} from its id.
	 * @param jobExecution the parent {@link JobExecution}
	 * @param stepExecutionId the step execution id
	 * @return a {@link StepExecution}
	 * @deprecated since 6.0 in favor of {@link #getStepExecution(long)}
	 */
	@Nullable
	@Deprecated(since = "6.0", forRemoval = true)
	StepExecution getStepExecution(JobExecution jobExecution, long stepExecutionId);

	/**
	 * Retrieve the last {@link StepExecution} for a given {@link JobInstance} ordered by
	 * creation time and then id.
	 * @param jobInstance the parent {@link JobInstance}
	 * @param stepName the name of the step
	 * @return a {@link StepExecution}
	 */
	@Nullable
	default StepExecution getLastStepExecution(JobInstance jobInstance, String stepName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Retrieve all {@link StepExecution}s for a given {@link JobExecution}. The execution
	 * context will not be loaded. If you need the execution context, use the job
	 * repository which coordinates the calls to the various DAOs.
	 * @param jobExecution the parent {@link JobExecution}
	 * @return a list of {@link StepExecution}s
	 * @since 6.0
	 */
	default List<StepExecution> getStepExecutions(JobExecution jobExecution) {
		return Collections.emptyList();
	}

	/**
	 * Counts all the {@link StepExecution} for a given step name.
	 * @param jobInstance the parent {@link JobInstance}
	 * @param stepName the name of the step
	 * @since 4.3
	 * @return the count of {@link StepExecution}s for a given step
	 */
	default long countStepExecutions(JobInstance jobInstance, String stepName) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Delete the given step execution.
	 * @param stepExecution the step execution to delete
	 * @since 5.0
	 */
	default void deleteStepExecution(StepExecution stepExecution) {
		throw new UnsupportedOperationException();
	}

}
