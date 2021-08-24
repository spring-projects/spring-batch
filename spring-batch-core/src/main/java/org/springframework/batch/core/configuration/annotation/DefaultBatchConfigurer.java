/*
 * Copyright 2012-2021 the original author or authors.
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

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.BatchConfigurationException;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

@Component
public class DefaultBatchConfigurer implements BatchConfigurer {

	private DataSource dataSource;
	private PlatformTransactionManager transactionManager;
	private JobRepository jobRepository;
	private JobLauncher jobLauncher;
	private JobExplorer jobExplorer;

	/**
	 * Sets the dataSource.
	 *
	 * @param dataSource The data source to use. Must not be {@code null}.
	 */
	public void setDataSource(DataSource dataSource) {
		Assert.notNull(dataSource, "DataSource must not be null");
		this.dataSource = dataSource;
	}

	public DataSource getDataSource() {
		return this.dataSource;
	}

	/**
	 * Create a new {@link DefaultBatchConfigurer} with the passed datasource. This constructor
	 * will configure a default {@link DataSourceTransactionManager}.
	 *
	 * @param dataSource to use for the job repository and job explorer
	 */
	public DefaultBatchConfigurer(DataSource dataSource) {
		this(dataSource, new DataSourceTransactionManager(dataSource));
	}

	/**
	 * Create a new {@link DefaultBatchConfigurer} with the passed datasource and transaction manager.
	 * @param dataSource to use for the job repository and job explorer
	 * @param transactionManager to use for the job repository
	 */
	public DefaultBatchConfigurer(DataSource dataSource, PlatformTransactionManager transactionManager) {
		Assert.notNull(dataSource, "DataSource must not be null");
		Assert.notNull(transactionManager, "transactionManager must not be null");
		this.dataSource = dataSource;
		this.transactionManager = transactionManager;
	}

	@Override
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() {
		return jobExplorer;
	}

	@PostConstruct
	public void initialize() {
		try {
			this.jobRepository = createJobRepository();
			this.jobExplorer = createJobExplorer();
			this.jobLauncher = createJobLauncher();
		} catch (Exception e) {
			throw new BatchConfigurationException(e);
		}
	}

	protected JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(this.jobRepository);
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(this.dataSource);
		factory.setTransactionManager(getTransactionManager());
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	protected JobExplorer createJobExplorer() throws Exception {
		JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
		jobExplorerFactoryBean.setDataSource(this.dataSource);
		jobExplorerFactoryBean.afterPropertiesSet();
		return jobExplorerFactoryBean.getObject();
	}
}
