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

package org.springframework.batch.integration.partition;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Convenient factory for a {@link RemotePartitioningWorkerStepBuilder} which sets
 * the {@link JobRepository}, {@link JobExplorer}, {@link BeanFactory} and
 * {@link PlatformTransactionManager} automatically.
 *
 * @since 4.1
 * @author Mahmoud Ben Hassine
 */
public class RemotePartitioningWorkerStepBuilderFactory implements BeanFactoryAware {

	private BeanFactory beanFactory;
	final private JobExplorer jobExplorer;
	final private JobRepository jobRepository;
	final private PlatformTransactionManager transactionManager;


	/**
	 * Create a new {@link RemotePartitioningWorkerStepBuilderFactory}.
	 * @param jobRepository the job repository to use
	 * @param jobExplorer the job explorer to use
	 * @param transactionManager the transaction manager to use
	 */
	public RemotePartitioningWorkerStepBuilderFactory(JobRepository jobRepository,
		JobExplorer jobExplorer,
		PlatformTransactionManager transactionManager) {

		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
		this.transactionManager = transactionManager;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	/**
	 * Creates a {@link RemotePartitioningWorkerStepBuilder} and initializes its job
	 * repository, job explorer, bean factory and transaction manager.
	 * @param name the name of the step
	 * @return a {@link RemotePartitioningWorkerStepBuilder}
	 */
	public RemotePartitioningWorkerStepBuilder get(String name) {
		return new RemotePartitioningWorkerStepBuilder(name)
				.repository(this.jobRepository)
				.jobExplorer(this.jobExplorer)
				.beanFactory(this.beanFactory)
				.transactionManager(this.transactionManager);
	}

}
