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

import java.util.concurrent.atomic.AtomicReference;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.AbstractLazyCreationTargetSource;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
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
 * available by implementing the {@link BatchConfigurer} interface. The main components are created as lazy proxies that
 * only initialize when a method is called. This is to prevent (as much as possible) configuration cycles from
 * developing when these components are needed in a configuration resource that itself provides a
 * {@link BatchConfigurer}.
 *
 * @author Dave Syer
 * @since 2.2
 * @see EnableBatchProcessing
 */
@Configuration(proxyBeanMethods = false)
public class SimpleBatchConfiguration extends AbstractBatchConfiguration {

	@Autowired
	private ApplicationContext context;

	private boolean initialized = false;

	private AtomicReference<JobRepository> jobRepository = new AtomicReference<>();

	private AtomicReference<JobLauncher> jobLauncher = new AtomicReference<>();

	private AtomicReference<JobRegistry> jobRegistry = new AtomicReference<>();

	private AtomicReference<PlatformTransactionManager> transactionManager = new AtomicReference<>();

	private AtomicReference<JobExplorer> jobExplorer = new AtomicReference<>();

	@Override
	@Bean
	public JobRepository jobRepository() throws Exception {
		return createLazyProxy(jobRepository, JobRepository.class);
	}

	@Override
	@Bean
	public JobLauncher jobLauncher() throws Exception {
		return createLazyProxy(jobLauncher, JobLauncher.class);
	}

	@Override
	@Bean
	public JobRegistry jobRegistry() throws Exception {
		return createLazyProxy(jobRegistry, JobRegistry.class);
	}

	@Override
	@Bean
	public JobExplorer jobExplorer() {
		return createLazyProxy(jobExplorer, JobExplorer.class);
	}

	@Override
	@Bean
	public PlatformTransactionManager transactionManager() throws Exception {
		return createLazyProxy(transactionManager, PlatformTransactionManager.class);
	}

	private <T> T createLazyProxy(AtomicReference<T> reference, Class<T> type) {
		ProxyFactory factory = new ProxyFactory();
		factory.setTargetSource(new ReferenceTargetSource<>(reference));
		factory.addAdvice(new PassthruAdvice());
		factory.setInterfaces(new Class<?>[] { type });
		@SuppressWarnings("unchecked")
		T proxy = (T) factory.getProxy();
		return proxy;
	}

	/**
	 * Sets up the basic components by extracting them from the {@link BatchConfigurer configurer}, defaulting to some
	 * sensible values as long as a unique DataSource is available.
	 *
	 * @throws Exception if there is a problem in the configurer
	 */
	protected void initialize() throws Exception {
		if (initialized) {
			return;
		}
		BatchConfigurer configurer = getConfigurer(context.getBeansOfType(BatchConfigurer.class).values());
		jobRepository.set(configurer.getJobRepository());
		jobLauncher.set(configurer.getJobLauncher());
		transactionManager.set(configurer.getTransactionManager());
		jobRegistry.set(new MapJobRegistry());
		jobExplorer.set(configurer.getJobExplorer());
		initialized = true;
	}

	private class PassthruAdvice implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			return invocation.proceed();
		}

	}

	private class ReferenceTargetSource<T> extends AbstractLazyCreationTargetSource {

		private AtomicReference<T> reference;

		public ReferenceTargetSource(AtomicReference<T> reference) {
			this.reference = reference;
		}

		@Override
		protected Object createObject() throws Exception {
			initialize();
			return reference.get();
		}
	}

}
