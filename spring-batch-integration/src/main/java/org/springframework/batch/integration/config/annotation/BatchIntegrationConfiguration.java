/*
 * Copyright 2018-2019 the original author or authors.
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
import org.springframework.batch.integration.chunk.RemoteChunkingManagerStepBuilderFactory;
import org.springframework.batch.integration.chunk.RemoteChunkingWorkerBuilder;
import org.springframework.batch.integration.partition.RemotePartitioningMasterStepBuilderFactory;
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
public class BatchIntegrationConfiguration implements InitializingBean {

	private JobExplorer jobExplorer;

	private JobRepository jobRepository;

	private PlatformTransactionManager transactionManager;

	private RemoteChunkingMasterStepBuilderFactory remoteChunkingMasterStepBuilderFactory;

	private RemoteChunkingManagerStepBuilderFactory remoteChunkingManagerStepBuilderFactory;

	private RemoteChunkingWorkerBuilder remoteChunkingWorkerBuilder;

	private RemotePartitioningMasterStepBuilderFactory remotePartitioningMasterStepBuilderFactory;

	private RemotePartitioningManagerStepBuilderFactory remotePartitioningManagerStepBuilderFactory;

	private RemotePartitioningWorkerStepBuilderFactory remotePartitioningWorkerStepBuilderFactory;

	@Autowired
	public BatchIntegrationConfiguration(
			JobRepository jobRepository,
			JobExplorer jobExplorer,
			PlatformTransactionManager transactionManager) {

		this.jobRepository = jobRepository;
		this.jobExplorer = jobExplorer;
		this.transactionManager = transactionManager;
	}

	@Deprecated
	@Bean
	public RemoteChunkingMasterStepBuilderFactory remoteChunkingMasterStepBuilderFactory() {
		return this.remoteChunkingMasterStepBuilderFactory;
	}

	@Bean
	public RemoteChunkingManagerStepBuilderFactory remoteChunkingManagerStepBuilderFactory() {
		return this.remoteChunkingManagerStepBuilderFactory;
	}

	@Bean
	public <I,O> RemoteChunkingWorkerBuilder<I, O> remoteChunkingWorkerBuilder() {
		return remoteChunkingWorkerBuilder;
	}

	@Deprecated
	@Bean
	public RemotePartitioningMasterStepBuilderFactory remotePartitioningMasterStepBuilderFactory() {
		return remotePartitioningMasterStepBuilderFactory;
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
		this.remoteChunkingMasterStepBuilderFactory  = new RemoteChunkingMasterStepBuilderFactory(this.jobRepository,
				this.transactionManager);
		this.remoteChunkingManagerStepBuilderFactory = new RemoteChunkingManagerStepBuilderFactory(this.jobRepository,
				this.transactionManager);
		this.remoteChunkingWorkerBuilder = new RemoteChunkingWorkerBuilder<>();
		this.remotePartitioningMasterStepBuilderFactory = new RemotePartitioningMasterStepBuilderFactory(this.jobRepository,
				this.jobExplorer, this.transactionManager);
		this.remotePartitioningManagerStepBuilderFactory = new RemotePartitioningManagerStepBuilderFactory(this.jobRepository,
				this.jobExplorer, this.transactionManager);
		this.remotePartitioningWorkerStepBuilderFactory = new RemotePartitioningWorkerStepBuilderFactory(this.jobRepository,
				this.jobExplorer, this.transactionManager);
	}
}
