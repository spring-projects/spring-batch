/*
 * Copyright 2018-2025 the original author or authors.
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

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilderFactory;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.partition.RemotePartitioningManagerStepBuilderFactory;
import org.springframework.batch.integration.partition.RemotePartitioningWorkerStepBuilderFactory;
import org.springframework.beans.factory.InitializingBean;
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
@Configuration(proxyBeanMethods = false)
public class BatchIntegrationConfiguration<I, O> implements InitializingBean {

	private final JobRepository jobRepository;

	private final PlatformTransactionManager transactionManager;

	private RemoteChunkingManagerStepBuilderFactory remoteChunkingManagerStepBuilderFactory;

	private RemoteChunkingWorkerBuilder<I, O> remoteChunkingWorkerBuilder;

	private RemotePartitioningManagerStepBuilderFactory remotePartitioningManagerStepBuilderFactory;

	private RemotePartitioningWorkerStepBuilderFactory remotePartitioningWorkerStepBuilderFactory;

	@Autowired
	public BatchIntegrationConfiguration(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
		this.jobRepository = jobRepository;
		this.transactionManager = transactionManager;
	}

	@Bean
	public RemoteChunkingManagerStepBuilderFactory remoteChunkingManagerStepBuilderFactory() {
		return this.remoteChunkingManagerStepBuilderFactory;
	}

	@Bean
	public RemoteChunkingWorkerBuilder<I, O> remoteChunkingWorkerBuilder() {
		return remoteChunkingWorkerBuilder;
	}

	@Bean
	public RemotePartitioningManagerStepBuilderFactory remotePartitioningManagerStepBuilderFactory() {
		return this.remotePartitioningManagerStepBuilderFactory;
	}

	@Bean
	public RemotePartitioningWorkerStepBuilderFactory remotePartitioningWorkerStepBuilderFactory() {
		return this.remotePartitioningWorkerStepBuilderFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.remoteChunkingManagerStepBuilderFactory = new RemoteChunkingManagerStepBuilderFactory(this.jobRepository,
				this.transactionManager);
		this.remoteChunkingWorkerBuilder = new RemoteChunkingWorkerBuilder<>();
		this.remotePartitioningManagerStepBuilderFactory = new RemotePartitioningManagerStepBuilderFactory(
				this.jobRepository);
		this.remotePartitioningWorkerStepBuilderFactory = new RemotePartitioningWorkerStepBuilderFactory(
				this.jobRepository);
	}

}
