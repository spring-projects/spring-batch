/*
 * Copyright 2019-2022 the original author or authors.
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
 * Convenient factory for a {@link RemoteChunkingManagerStepBuilder} which sets the
 * {@link JobRepository} and {@link PlatformTransactionManager} automatically.
 *
 * @since 4.2
 * @author Mahmoud Ben Hassine
 */
public class RemoteChunkingManagerStepBuilderFactory {

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	/**
	 * Create a new {@link RemoteChunkingManagerStepBuilderFactory}.
	 * @param jobRepository the job repository to use
	 * @param transactionManager the transaction manager to use
	 */
	public RemoteChunkingManagerStepBuilderFactory(JobRepository jobRepository,
			PlatformTransactionManager transactionManager) {

		this.jobRepository = jobRepository;
		this.transactionManager = transactionManager;
	}

	/**
	 * Creates a {@link RemoteChunkingManagerStepBuilder} and initializes its job
	 * repository and transaction manager.
	 * @param name the name of the step
	 * @param <I> type of input items
	 * @param <O> type of output items
	 * @return a {@link RemoteChunkingManagerStepBuilder}
	 */
	public <I, O> RemoteChunkingManagerStepBuilder<I, O> get(String name) {
		return new RemoteChunkingManagerStepBuilder<I, O>(name, this.jobRepository)
			.transactionManager(this.transactionManager);
	}

}
