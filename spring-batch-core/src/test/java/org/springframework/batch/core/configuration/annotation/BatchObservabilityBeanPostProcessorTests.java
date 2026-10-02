/*
 * Copyright 2026 the original author or authors.
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
import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Test class for {@link BatchObservabilityBeanPostProcessor}.
 *
 * @author Sanghyuk Jung
 * @author Yanming Zhou
 */
class BatchObservabilityBeanPostProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final ObservationRegistry observationRegistry = ObservationRegistry.create();

	private final BatchObservabilityBeanPostProcessor postProcessor = new BatchObservabilityBeanPostProcessor();

	@Test
	void observationRegistryShouldBeSetOnJob() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		SimpleJob job = new SimpleJob();

		// when
		this.postProcessor.postProcessAfterInitialization(job, "job");

		// then
		assertSame(this.observationRegistry, ReflectionTestUtils.getField(job, "observationRegistry"));
	}

	@Test
	void observationRegistryShouldBeSetOnStep() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		TaskletStep step = new TaskletStep(mock(JobRepository.class));

		// when
		this.postProcessor.postProcessAfterInitialization(step, "step");

		// then
		assertSame(this.observationRegistry, ReflectionTestUtils.getField(step, "observationRegistry"));
	}

	@Test
	void observationRegistryShouldBeSetOnJobOperator() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		TaskExecutorJobOperator jobOperator = new TaskExecutorJobOperator();

		// when
		this.postProcessor.postProcessAfterInitialization(jobOperator, "jobOperator");

		// then
		assertSame(this.observationRegistry, ReflectionTestUtils.getField(jobOperator, "observationRegistry"));
	}

	@Test
	void observationRegistryShouldBeSetOnProxiedJobOperator() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		TaskExecutorJobOperator target = new TaskExecutorJobOperator();
		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTarget(target);
		proxyFactory.setProxyTargetClass(false);
		proxyFactory.addInterface(JobOperator.class);
		Object jobOperator = proxyFactory.getProxy();

		// when
		this.postProcessor.postProcessAfterInitialization(jobOperator, "jobOperator");

		// then
		assertSame(this.observationRegistry, ReflectionTestUtils.getField(target, "observationRegistry"));
	}

	@Test
	void observationRegistryShouldNotBeSetOnJobIfConfigured() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		ObservationRegistry usedObservationRegistry = mock(ObservationRegistry.class);
		SimpleJob job = new SimpleJob();
		job.setObservationRegistry(usedObservationRegistry);

		// when
		this.postProcessor.postProcessAfterInitialization(job, "job");

		// then
		assertSame(usedObservationRegistry, ReflectionTestUtils.getField(job, "observationRegistry"));
	}

	@Test
	void observationRegistryShouldNotBeSetOnStepIfConfigured() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		ObservationRegistry usedObservationRegistry = mock(ObservationRegistry.class);
		TaskletStep step = new TaskletStep(mock(JobRepository.class));
		step.setObservationRegistry(usedObservationRegistry);

		// when
		this.postProcessor.postProcessAfterInitialization(step, "step");

		// then
		assertSame(usedObservationRegistry, ReflectionTestUtils.getField(step, "observationRegistry"));
	}

	@Test
	void observationRegistryShouldNotBeSetOnJobOperatorIfConfigured() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		TaskExecutorJobOperator jobOperator = new TaskExecutorJobOperator();
		ObservationRegistry usedObservationRegistry = mock(ObservationRegistry.class);
		jobOperator.setObservationRegistry(usedObservationRegistry);

		// when
		this.postProcessor.postProcessAfterInitialization(jobOperator, "jobOperator");

		// then
		assertSame(usedObservationRegistry, ReflectionTestUtils.getField(jobOperator, "observationRegistry"));
	}

}
