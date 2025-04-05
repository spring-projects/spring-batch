/*
 * Copyright 2022-2025 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;

/**
 * A {@link org.springframework.core.task.TaskExecutor}-based implementation of the
 * {@link JobOperator} interface. The following dependencies are required:
 *
 * <ul>
 * <li>{@link JobRepository}
 * <li>{@link JobRegistry}
 * </ul>
 *
 * This class can be instantiated with a {@link JobOperatorFactoryBean} to create a
 * transactional proxy around the job operator.
 *
 * @see JobOperatorFactoryBean
 * @author Dave Syer
 * @author Lucas Ward
 * @author Will Schipp
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
@SuppressWarnings("removal")
public class TaskExecutorJobOperator extends SimpleJobOperator {

	/**
	 * Public setter for the {@link ListableJobLocator}.
	 * @param jobRegistry the {@link ListableJobLocator} to set
	 */
	@Override
	public void setJobRegistry(ListableJobLocator jobRegistry) {
		this.jobRegistry = jobRegistry;
	}

}
