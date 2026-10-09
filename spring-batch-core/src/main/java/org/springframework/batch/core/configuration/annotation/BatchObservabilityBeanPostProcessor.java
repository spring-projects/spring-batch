/*
 * Copyright 2022-present the original author or authors.
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

import java.util.concurrent.atomic.AtomicBoolean;

import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * Bean post processor that configures observable batch artifacts (typically jobs and
 * steps) with a Micrometer's observation registry.
 * <p>
 * The observation registry is only set on artifacts that have none configured. An
 * observation registry that was set explicitly on a job, a step or a job operator
 * (including {@link ObservationRegistry#NOOP}) is left untouched.
 * <p>
 * If the application context contains several {@link ObservationRegistry} beans and none
 * of them is {@link org.springframework.context.annotation.Primary primary}, no registry
 * is set and a warning is logged.
 *
 * @author Mahmoud Ben Hassine
 * @author Sanghyuk Jung
 * @since 5.0
 */
public class BatchObservabilityBeanPostProcessor implements BeanFactoryPostProcessor, BeanPostProcessor {

	private static final Log LOGGER = LogFactory.getLog(BatchObservabilityBeanPostProcessor.class);

	private final AtomicBoolean ambiguityReported = new AtomicBoolean();

	private @Nullable ConfigurableListableBeanFactory beanFactory;

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (this.beanFactory == null) {
			LOGGER.debug("BeanFactory is not initialized, skipping observation registry injection");
			return bean;
		}
		try {
			Object target = AopProxyUtils.getSingletonTarget(bean);
			if (target == null) {
				target = bean;
			}
			if (target instanceof AbstractJob || target instanceof AbstractStep
					|| target instanceof TaskExecutorJobOperator) {
				ObservationRegistry observationRegistry = this.beanFactory.getBean(ObservationRegistry.class);
				if (target instanceof AbstractJob job && job.getObservationRegistry() == null) {
					job.setObservationRegistry(observationRegistry);
				}
				if (target instanceof AbstractStep step && step.getObservationRegistry() == null) {
					step.setObservationRegistry(observationRegistry);
				}
				if (target instanceof TaskExecutorJobOperator operator && operator.getObservationRegistry() == null) {
					operator.setObservationRegistry(observationRegistry);
				}
			}
		}
		catch (NoUniqueBeanDefinitionException e) {
			if (this.ambiguityReported.compareAndSet(false, true)) {
				LOGGER.warn("Found " + e.getNumberOfBeansFound() + " Micrometer observation registries ("
						+ e.getBeanNamesFound() + "), none of them primary: bean '" + beanName
						+ "' and any other batch artifact without an explicit observation registry will use"
						+ " ObservationRegistry.NOOP. Mark one of them as @Primary to use it by default.");
			}
		}
		catch (NoSuchBeanDefinitionException e) {
			LOGGER.debug("No Micrometer observation registry found, defaulting to ObservationRegistry.NOOP");
		}
		return bean;
	}

}
