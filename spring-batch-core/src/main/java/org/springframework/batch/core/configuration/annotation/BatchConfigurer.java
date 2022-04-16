/*
 * Copyright 2012-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.annotation;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Strategy interface for users to provide as a factory for custom components needed by a Batch system.
 * 
 * @author Dave Syer
 * 
 */
public interface BatchConfigurer {

	/**
	 * @return The {@link JobRepository}.
	 * @throws Exception The {@link Exception} thrown if an error occurs.
	 */
	JobRepository getJobRepository() throws Exception;

	/**
	 * @return The {@link PlatformTransactionManager}.
	 * @throws Exception The {@link Exception} thrown if an error occurs.
	 */
	PlatformTransactionManager getTransactionManager() throws Exception;

	/**
	 * @return The {@link JobLauncher}.
	 * @throws Exception The {@link Exception} thrown if an error occurs.
	 */
	JobLauncher getJobLauncher() throws Exception;

	/**
	 * @return The {@link JobExplorer}.
	 * @throws Exception The {@link Exception} thrown if an error occurs.
	 */
	JobExplorer getJobExplorer() throws Exception;
}
