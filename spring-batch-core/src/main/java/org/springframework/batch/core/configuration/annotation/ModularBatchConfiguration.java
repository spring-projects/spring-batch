/*
 * Copyright 2012-2013 the original author or authors.
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

import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.DefaultJobLoader;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Base {@code Configuration} class providing common structure for enabling and using Spring Batch. Customization is
 * available by implementing the {@link BatchConfigurer} interface.
 *
 * @author Dave Syer
 * @since 2.2
 * @see EnableBatchProcessing
 */
@Configuration(proxyBeanMethods = false)
public class ModularBatchConfiguration extends AbstractBatchConfiguration {

	@Autowired
	private ApplicationContext context;

	@Autowired(required = false)
	private Collection<BatchConfigurer> configurers;

	private AutomaticJobRegistrar registrar = new AutomaticJobRegistrar();

	@Override
	@Bean
	public JobRepository jobRepository() throws Exception {
		return getConfigurer(configurers).getJobRepository();
	}

	@Override
	@Bean
	public JobLauncher jobLauncher() throws Exception {
		return getConfigurer(configurers).getJobLauncher();
	}

	@Override
	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		return getConfigurer(configurers).getTransactionManager();
	}

	@Override
	@Bean
	public JobExplorer jobExplorer() throws Exception {
		return getConfigurer(configurers).getJobExplorer();
	}

	@Bean
	public AutomaticJobRegistrar jobRegistrar() throws Exception {
		registrar.setJobLoader(new DefaultJobLoader(jobRegistry()));
		for (ApplicationContextFactory factory : context.getBeansOfType(ApplicationContextFactory.class).values()) {
			registrar.addApplicationContextFactory(factory);
		}
		return registrar;
	}

}
