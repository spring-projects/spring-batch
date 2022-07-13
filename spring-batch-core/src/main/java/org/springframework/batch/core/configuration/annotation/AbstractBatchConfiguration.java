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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base {@code Configuration} class providing common structure for enabling and using
 * Spring Batch. Customization is available by implementing the {@link BatchConfigurer}
 * interface.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2
 * @see EnableBatchProcessing
 */
@Configuration(proxyBeanMethods = false)
@Import(ScopeConfiguration.class)
public abstract class AbstractBatchConfiguration implements InitializingBean {

	private static final Log logger = LogFactory.getLog(AbstractBatchConfiguration.class);

	@Autowired
	protected ApplicationContext context;

	private JobBuilderFactory jobBuilderFactory;

	private StepBuilderFactory stepBuilderFactory;

	private JobRegistry jobRegistry = new MapJobRegistry();

	/**
	 * Establish the {@link JobBuilderFactory} for the batch execution.
	 * @return The instance of the {@link JobBuilderFactory}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public JobBuilderFactory jobBuilders() throws Exception {
		return this.jobBuilderFactory;
	}

	/**
	 * Establish the {@link StepBuilderFactory} for the batch execution.
	 * @return The instance of the {@link StepBuilderFactory}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public StepBuilderFactory stepBuilders() throws Exception {
		return this.stepBuilderFactory;
	}

	/**
	 * Establish the {@link JobRepository} for the batch execution.
	 * @return The instance of the {@link JobRepository}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public abstract JobRepository jobRepository() throws Exception;

	/**
	 * Establish the {@link JobLauncher} for the batch execution.
	 * @return The instance of the {@link JobLauncher}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public abstract JobLauncher jobLauncher() throws Exception;

	/**
	 * Establish the {@link JobExplorer} for the batch execution.
	 * @return The instance of the {@link JobExplorer}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public abstract JobExplorer jobExplorer() throws Exception;

	/**
	 * Establish the {@link JobRegistry} for the batch execution.
	 * @return The instance of the {@link JobRegistry}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public JobRegistry jobRegistry() throws Exception {
		return this.jobRegistry;
	}

	/**
	 * Establish the {@link PlatformTransactionManager} for the batch execution.
	 * @return The instance of the {@link PlatformTransactionManager}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	public abstract PlatformTransactionManager transactionManager() throws Exception;

	@Override
	public void afterPropertiesSet() throws Exception {
		BatchConfigurer batchConfigurer = getOrCreateConfigurer();
		this.jobBuilderFactory = new JobBuilderFactory(batchConfigurer.getJobRepository());
		this.stepBuilderFactory = new StepBuilderFactory(batchConfigurer.getJobRepository(),
				batchConfigurer.getTransactionManager());
	}

	/**
	 * If a {@link BatchConfigurer} exists, return it. Otherwise, create a
	 * {@link DefaultBatchConfigurer}. If more than one configurer is present, an
	 * {@link IllegalStateException} is thrown.
	 * @return The {@link BatchConfigurer} that was in the configurers collection or the
	 * default one created.
	 */
	protected BatchConfigurer getOrCreateConfigurer() {
		BatchConfigurer batchConfigurer = getConfigurer();
		if (batchConfigurer == null) {
			batchConfigurer = createDefaultConfigurer();
		}
		return batchConfigurer;
	}

	private BatchConfigurer getConfigurer() {
		Map<String, BatchConfigurer> configurers = this.context.getBeansOfType(BatchConfigurer.class);
		if (configurers != null && configurers.size() > 1) {
			throw new IllegalStateException(
					"To use a custom BatchConfigurer the context must contain precisely one, found "
							+ configurers.size());
		}
		if (configurers != null && configurers.size() == 1) {
			return configurers.entrySet().iterator().next().getValue();
		}
		return null;
	}

	private BatchConfigurer createDefaultConfigurer() {
		DataSource dataSource = getDataSource();
		DefaultBatchConfigurer configurer = new DefaultBatchConfigurer(dataSource);
		configurer.initialize();
		return configurer;
	}

	private DataSource getDataSource() {
		Map<String, DataSource> dataSources = this.context.getBeansOfType(DataSource.class);
		if (dataSources == null || (dataSources != null && dataSources.isEmpty())) {
			throw new IllegalStateException("To use the default BatchConfigurer, the application context must"
					+ " contain at least one data source but none was found.");
		}
		if (dataSources != null && dataSources.size() > 1) {
			logger.info("Multiple data sources are defined in the application context. The data source to"
					+ " use in the default BatchConfigurer will be the one selected by Spring according"
					+ " to the rules of getting the primary bean from the application context.");
			return this.context.getBean(DataSource.class);
		}
		return dataSources.entrySet().iterator().next().getValue();
	}

}
