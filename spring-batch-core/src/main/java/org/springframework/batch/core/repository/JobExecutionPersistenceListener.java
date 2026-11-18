/*
 * Copyright 2026-present the original author or authors.
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
package org.springframework.batch.core.repository;

import org.springframework.batch.core.job.JobExecution;

/**
 * Listener for changes to job execution metadata made by a JDBC job repository. Callbacks
 * run synchronously in the repository transaction, after the corresponding write succeeds
 * and before commit. With the default transaction attributes, a runtime exception thrown
 * from a callback rolls back the write and any related writes in the same transaction.
 *
 * @since 6.1
 */
public interface JobExecutionPersistenceListener {

	/**
	 * Called after creating a job execution, including on restart.
	 * @param execution the newly persisted execution
	 */
	default void afterCreate(JobExecution execution) {
	}

	/**
	 * Called after {@link JobRepository#update(JobExecution)} persists a job execution.
	 * This method can be called more than once with the same status.
	 * @param execution the updated execution
	 */
	default void afterUpdate(JobExecution execution) {
	}

}
