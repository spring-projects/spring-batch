/*
 * Copyright 2012-2018 the original author or authors.
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
import org.springframework.batch.core.scope.JobScope;
import org.springframework.batch.core.scope.StepScope;
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

	@Autowired(required = false)
	private DataSource dataSource;

	private BatchConfigurer configurer;

	private JobRegistry jobRegistry = new MapJobRegistry();

	private JobBuilderFactory jobBuilderFactory;

	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public JobBuilderFactory jobBuilders() throws Exception {
		return this.jobBuilderFactory;
	}

	@Bean
	public StepBuilderFactory stepBuilders() throws Exception {
		return this.stepBuilderFactory;
	}

	@Bean
	public abstract JobRepository jobRepository() throws Exception;

	@Bean
	public abstract JobLauncher jobLauncher() throws Exception;

	@Bean
	public abstract JobExplorer jobExplorer() throws Exception;

	@Bean
	public JobRegistry jobRegistry() throws Exception {
		return this.jobRegistry;
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

	@Override
	public void afterPropertiesSet() throws Exception {
		this.jobBuilderFactory = new JobBuilderFactory(jobRepository());
		this.stepBuilderFactory = new StepBuilderFactory(jobRepository(), transactionManager());
	}

	protected BatchConfigurer getConfigurer(Collection<BatchConfigurer> configurers) throws Exception {
		if (this.configurer != null) {
			return this.configurer;
		}
		if (configurers == null || configurers.isEmpty()) {
			if (dataSource == null) {
				DefaultBatchConfigurer configurer = new DefaultBatchConfigurer();
				configurer.initialize();
				this.configurer = configurer;
				return configurer;
			} else {
				DefaultBatchConfigurer configurer = new DefaultBatchConfigurer(dataSource);
				configurer.initialize();
				this.configurer = configurer;
				return configurer;
			}
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
 * Extract job/step scope configuration into a separate unit.
 * 
 * @author Dave Syer
 * 
 */
@Configuration(proxyBeanMethods = false)
class ScopeConfiguration {

	private static StepScope stepScope;

	private static JobScope jobScope;

	static {
		jobScope = new JobScope();
		jobScope.setAutoProxy(false);

		stepScope = new StepScope();
		stepScope.setAutoProxy(false);
	}

	@Bean
	public static StepScope stepScope() {
		return stepScope;
	}

	@Bean
	public static JobScope jobScope() {
		return jobScope;
	}
}
