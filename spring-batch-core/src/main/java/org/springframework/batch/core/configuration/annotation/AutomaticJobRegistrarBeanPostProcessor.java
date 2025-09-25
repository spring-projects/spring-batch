/*
 * Copyright 2022-2025 the original author or authors.
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

import org.jspecify.annotations.NullUnmarked;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.support.ApplicationContextFactory;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.DefaultJobLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Post processor that configures the {@link AutomaticJobRegistrar} registered by
 * {@link BatchRegistrar} with required properties.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 * @deprecated since 6.0 with no replacement. Scheduled for removal in 6.2 or later.
 */
@NullUnmarked
@Deprecated(since = "6.0", forRemoval = true)
class AutomaticJobRegistrarBeanPostProcessor implements BeanFactoryPostProcessor, BeanPostProcessor {

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof AutomaticJobRegistrar automaticJobRegistrar) {
			automaticJobRegistrar.setJobLoader(new DefaultJobLoader(this.beanFactory.getBean(JobRegistry.class)));
			for (ApplicationContextFactory factory : this.beanFactory.getBeansOfType(ApplicationContextFactory.class)
				.values()) {
				automaticJobRegistrar.addApplicationContextFactory(factory);
			}
			return automaticJobRegistrar;
		}
		return bean;
	}

}
