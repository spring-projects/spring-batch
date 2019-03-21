/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.batch.integration.chunk;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Convenient factory for a {@link RemoteChunkingMasterStepBuilder} which sets
 * the {@link JobRepository} and {@link PlatformTransactionManager} automatically.
 *
 * @since 4.1
 * @author Mahmoud Ben Hassine
 */
public class RemoteChunkingMasterStepBuilderFactory {

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	/**
	 * Create a new {@link RemoteChunkingMasterStepBuilderFactory}.
	 *
	 * @param jobRepository the job repository to use
	 * @param transactionManager the transaction manager to use
	 */
	public RemoteChunkingMasterStepBuilderFactory(
			JobRepository jobRepository,
			PlatformTransactionManager transactionManager) {

		this.jobRepository = jobRepository;
		this.transactionManager = transactionManager;
	}

	/**
	 * Creates a {@link RemoteChunkingMasterStepBuilder} and initializes its job
	 * repository and transaction manager.
	 * 
	 * @param name the name of the step
	 * @param <I> type of input items
	 * @param <O> type of output items
	 * @return a {@link RemoteChunkingMasterStepBuilder}
	 */
	public <I, O> RemoteChunkingMasterStepBuilder<I, O> get(String name) {
		return new RemoteChunkingMasterStepBuilder<I, O>(name)
				.repository(this.jobRepository)
				.transactionManager(this.transactionManager);
	}

}
