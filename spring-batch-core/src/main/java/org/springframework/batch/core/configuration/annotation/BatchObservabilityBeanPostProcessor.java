/*
 * Copyright 2022 the original author or authors.
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

import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Bean post processor that configures observable batch artifacts (jobs and steps) with
 * Micrometer's observation registry.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class BatchObservabilityBeanPostProcessor implements BeanFactoryPostProcessor, BeanPostProcessor {

	private static final Log LOGGER = LogFactory.getLog(BatchObservabilityBeanPostProcessor.class);

	private ConfigurableListableBeanFactory beanFactory;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		try {
			if (bean instanceof AbstractJob || bean instanceof AbstractStep) {
				ObservationRegistry observationRegistry = this.beanFactory.getBean(ObservationRegistry.class);
				if (bean instanceof AbstractJob job) {
					job.setObservationRegistry(observationRegistry);
				}
				if (bean instanceof AbstractStep step) {
					step.setObservationRegistry(observationRegistry);
				}
			}
		}
		catch (NoSuchBeanDefinitionException e) {
			LOGGER.info("No Micrometer observation registry found, defaulting to ObservationRegistry.NOOP");
		}
		return bean;
	}

}
