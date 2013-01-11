/*
 * Copyright 2012-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
 * @since 2.2
 * @see EnableBatchProcessing
 */
@Configuration
@Import(StepScopeConfiguration.class)
public abstract class AbstractBatchConfiguration implements ImportAware {

	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private Collection<DataSource> dataSources;

	private BatchConfigurer configurer;

	@Bean
	public JobBuilderFactory jobBuilders() throws Exception {
		return new JobBuilderFactory(jobRepository());
	}

	@Bean
	public StepBuilderFactory stepBuilders() throws Exception {
		return new StepBuilderFactory(jobRepository(), transactionManager());
	}

	@Bean
	public abstract JobRepository jobRepository() throws Exception;

	@Bean
	public abstract JobLauncher jobLauncher() throws Exception;

	@Bean
	public JobRegistry jobRegistry() throws Exception {
		return new MapJobRegistry();
	}

	@Bean
	public abstract PlatformTransactionManager transactionManager() throws Exception;

	@Override
	public void setImportMetadata(AnnotationMetadata importMetadata) {
		AnnotationAttributes enabled = AnnotationAttributes.fromMap(importMetadata.getAnnotationAttributes(
				EnableBatchProcessing.class.getName(), false));
		Assert.notNull(enabled,
				"@EnableBatchProcessing is not present on importing class " + importMetadata.getClassName());
	}

	protected BatchConfigurer getConfigurer(Collection<BatchConfigurer> configurers) throws Exception {
		if (this.configurer != null) {
			return this.configurer;
		}
		if (configurers == null || configurers.isEmpty()) {
			if (dataSources == null || dataSources.isEmpty() || dataSources.size() > 1) {
				throw new IllegalStateException(
						"To use the default BatchConfigurer the context must contain precisely one DataSource, found "
								+ (dataSources == null ? 0 : dataSources.size()));
			}
			DataSource dataSource = dataSources.iterator().next();
			DefaultBatchConfigurer configurer = new DefaultBatchConfigurer(dataSource);
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

/**
 * Extract step scope configuration into a separate unit so that it can be non-static.
 * 
 * @author Dave Syer
 * 
 */
@Configuration
class StepScopeConfiguration {

	private StepScope stepScope = new StepScope();

	@Bean
	public StepScope stepScope() {
		stepScope.setAutoProxy(false);
		return stepScope;
	}

}