/*
 * Copyright 2006-2013 the original author or authors.
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

import java.util.Collection;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;

/**
 * DAO interface for persisting and retrieving {@link ExecutionContext}s.
 * 
 * @author Robert Kasanicky
 * @author David Turanski
 */
public interface ExecutionContextDao {

	/**
	 * @param jobExecution {@link JobExecution} instance that contains the context.
	 * @return execution context associated with the given jobExecution
	 */
	ExecutionContext getExecutionContext(JobExecution jobExecution);

	/**
	 * @param stepExecution {@link StepExecution} instance that contains the context.
	 * @return execution context associated with the given stepExecution
	 */
	ExecutionContext getExecutionContext(StepExecution stepExecution);

	/**
	 * Persist the execution context associated with the given jobExecution,
	 * persistent entry for the context should not exist yet.
	 *
	 * @param jobExecution {@link JobExecution} instance that contains the context.
	 */
	void saveExecutionContext(final JobExecution jobExecution);

	/**
	 * Persist the execution context associated with the given stepExecution,
	 * persistent entry for the context should not exist yet.
	 *
	 * @param stepExecution {@link StepExecution} instance that contains the context.
	 */
	void saveExecutionContext(final StepExecution stepExecution);

	/**
	 * Persist the execution context associated with each stepExecution in a given collection,
	 * persistent entry for the context should not exist yet.
	 *
	 * @param stepExecutions a collection of {@link StepExecution}s that contain
	 * the contexts.
	 */
	void saveExecutionContexts(final Collection<StepExecution> stepExecutions);

	/**
	 * Persist the updates of execution context associated with the given
	 * jobExecution. Persistent entry should already exist for this context.
	 *
	 * @param jobExecution {@link JobExecution} instance that contains the context.
	 */
	void updateExecutionContext(final JobExecution jobExecution);

	/**
	 * Persist the updates of execution context associated with the given
	 * stepExecution. Persistent entry should already exist for this context.
	 *
	 * @param stepExecution {@link StepExecution} instance that contains the context.
	 */
	void updateExecutionContext(final StepExecution stepExecution);
}
