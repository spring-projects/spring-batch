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
package org.springframework.batch.test.context;

import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.Assert;

/**
 * {@link ContextCustomizer} implementation that adds batch test utility classes
 * ({@link JobLauncherTestUtils} and {@link JobRepositoryTestUtils}) as beans in
 * the test context.
 *
 * @author Mahmoud Ben Hassine
 * @since 4.1
 */
public class BatchTestContextCustomizer implements ContextCustomizer {

	private static final String JOB_LAUNCHER_TEST_UTILS_BEAN_NAME = "jobLauncherTestUtils";
	private static final String JOB_REPOSITORY_TEST_UTILS_BEAN_NAME = "jobRepositoryTestUtils";

	@Override
	public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
		Assert.isInstanceOf(BeanDefinitionRegistry.class, beanFactory,
				"The bean factory must be an instance of BeanDefinitionRegistry");
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		registry.registerBeanDefinition(JOB_LAUNCHER_TEST_UTILS_BEAN_NAME,
				new RootBeanDefinition(JobLauncherTestUtils.class));
		registry.registerBeanDefinition(JOB_REPOSITORY_TEST_UTILS_BEAN_NAME,
				new RootBeanDefinition(JobRepositoryTestUtils.class));
	}

}
