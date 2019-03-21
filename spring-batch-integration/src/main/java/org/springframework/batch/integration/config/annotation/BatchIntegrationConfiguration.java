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
package org.springframework.batch.integration.config.annotation;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.chunk.RemoteChunkingMasterStepBuilderFactory;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base configuration class for Spring Batch Integration factory beans.
 *
 * @since 4.1
 * @author Mahmoud Ben Hassine
 */
@Configuration
public class BatchIntegrationConfiguration {

	private JobExplorer jobExplorer;

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	@Autowired
	public BatchIntegrationConfiguration(
			JobRepository jobRepository,
			JobExplorer jobExplorer,
			PlatformTransactionManager transactionManager) {

		this.jobRepository = jobRepository;
		this.jobExplorer = jobExplorer;
		this.transactionManager = transactionManager;
	}

	@Bean
	public RemoteChunkingMasterStepBuilderFactory remoteChunkingMasterStepBuilderFactory() {
		return new RemoteChunkingMasterStepBuilderFactory(this.jobRepository,
				this.transactionManager);
	}

	@Bean
	public <I,O> RemoteChunkingWorkerBuilder<I, O> remoteChunkingWorkerBuilder() {
		return new RemoteChunkingWorkerBuilder<>();
	}

	@Bean
	public RemotePartitioningMasterStepBuilderFactory remotePartitioningMasterStepBuilderFactory() {
		return new RemotePartitioningMasterStepBuilderFactory(this.jobRepository,
				this.jobExplorer, this.transactionManager);
	}

	@Bean
	public RemotePartitioningWorkerStepBuilderFactory remotePartitioningWorkerStepBuilderFactory() {
		return new RemotePartitioningWorkerStepBuilderFactory(this.jobRepository,
				this.jobExplorer, this.transactionManager);
	}

}
