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

import java.util.Collection;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Base {@code Configuration} class providing common structure for enabling and using Spring Batch. Customization is
 * available by implementing the {@link BatchConfigurer} interface. {@link BatchConfigurer}.
 * 
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.2
 * @see EnableBatchProcessing
 */
@Configuration(proxyBeanMethods = false)
@Import(ScopeConfiguration.class)
public abstract class AbstractBatchConfiguration implements ImportAware, InitializingBean {

	@Autowired
	private DataSource dataSource;

	private BatchConfigurer configurer;

	private JobRegistry jobRegistry = new MapJobRegistry();

	private JobBuilderFactory jobBuilderFactory;

	private StepBuilderFactory stepBuilderFactory;

	/**
	 * Establish the {@link JobBuilderFactory} for the batch execution.
	 *
	 * @return The instance of the {@link JobBuilderFactory}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public JobBuilderFactory jobBuilders() throws Exception {
		return this.jobBuilderFactory;
	}

	/**
	 * Establish the {@link StepBuilderFactory} for the batch execution.
	 *
	 * @return The instance of the {@link StepBuilderFactory}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public StepBuilderFactory stepBuilders() throws Exception {
		return this.stepBuilderFactory;
	}

	/**
	 * Establish the {@link JobRepository} for the batch execution.
	 *
	 * @return The instance of the {@link JobRepository}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public abstract JobRepository jobRepository() throws Exception;

	/**
	 * Establish the {@link JobLauncher} for the batch execution.
	 *
	 * @return The instance of the {@link JobLauncher}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public abstract JobLauncher jobLauncher() throws Exception;

	/**
	 * Establish the {@link JobExplorer} for the batch execution.
	 *
	 * @return The instance of the {@link JobExplorer}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public abstract JobExplorer jobExplorer() throws Exception;

	/**
	 * Establish the {@link JobRegistry} for the batch execution.
	 *
	 * @return The instance of the {@link JobRegistry}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	@Bean
	public JobRegistry jobRegistry() throws Exception {
		return this.jobRegistry;
	}

	/**
	 * Establish the {@link PlatformTransactionManager} for the batch execution.
	 *
	 * @return The instance of the {@link PlatformTransactionManager}.
	 * @throws Exception The {@link Exception} thrown if error occurs.
	 */
	public abstract PlatformTransactionManager transactionManager() throws Exception;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes enabled = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(
				EnableBatchProcessing.class.getName(), false));
		Assert.notNull(enabled,
				"@EnableBatchProcessing is not present on importing class " + importMetadata.getClassName());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jobBuilderFactory = new JobBuilderFactory(jobRepository());
		this.stepBuilderFactory = new StepBuilderFactory(jobRepository(), transactionManager());
	}

	/**
	 * If a {@link BatchConfigurer} exists, return it. If the configurers list is empty, create a {@link DefaultBatchConfigurer}.
	 * If more than one configurer is present in the list, an {@link IllegalStateException} is thrown.
	 * @param configurers The {@link Collection} of configurers to review.
	 * @return The {@link BatchConfigurer} that was in the configurers collection or the one created.
	 */
	protected BatchConfigurer getConfigurer(Collection<BatchConfigurer> configurers) {
		if (this.configurer != null) {
			return this.configurer;
		}
		if (configurers == null || configurers.isEmpty()) {
			DefaultBatchConfigurer configurer = new DefaultBatchConfigurer(this.dataSource);
			configurer.initialize();
			this.configurer = configurer;
			return configurer;
		}
		if (configurers.size() > 1) {
			throw new IllegalStateException(
					"To use a custom BatchConfigurer the context must contain precisely one, found "
							+ configurers.size());
		}
		this.configurer = configurers.iterator().next();
		return this.configurer;
	}

}
