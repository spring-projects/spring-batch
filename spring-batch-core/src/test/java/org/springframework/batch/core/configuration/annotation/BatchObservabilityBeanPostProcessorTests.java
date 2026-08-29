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
import org.springframework.batch.core.observability.BatchEventRecorder;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

/**
 * Test class for {@link BatchObservabilityBeanPostProcessor}.
 *
 * @author Sanghyuk Jung
 * @author Fabio Molignoni
 */
class BatchObservabilityBeanPostProcessorTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

	private final ObservationRegistry observationRegistry = ObservationRegistry.create();

	private final BatchEventRecorder batchEventRecorder = mock();

	private final BatchObservabilityBeanPostProcessor postProcessor = new BatchObservabilityBeanPostProcessor();

	@Test
	void observabilityComponentsShouldBeSetOnJob() {
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.beanFactory.registerSingleton("batchEventRecorder", this.batchEventRecorder);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		SimpleJob job = new SimpleJob("job");
		job.setJobRepository(new ResourcelessJobRepository());

		this.postProcessor.postProcessAfterInitialization(job, "job");

		assertSame(this.observationRegistry, ReflectionTestUtils.getField(job, "observationRegistry"));
		assertSame(this.batchEventRecorder, ReflectionTestUtils.getField(job, "batchEventRecorder"));
	}

	@Test
	void observabilityComponentsShouldBeSetOnStep() {
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.beanFactory.registerSingleton("batchEventRecorder", this.batchEventRecorder);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		TaskletStep step = new TaskletStep(new ResourcelessJobRepository());
		step.setName("step");

		this.postProcessor.postProcessAfterInitialization(step, "step");

		assertSame(this.observationRegistry, ReflectionTestUtils.getField(step, "observationRegistry"));
		assertSame(this.batchEventRecorder, ReflectionTestUtils.getField(step, "batchEventRecorder"));
	}

	@Test
	void observabilityComponentsShouldBeSetOnJobOperator() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.beanFactory.registerSingleton("batchEventRecorder", this.batchEventRecorder);
		this.postProcessor.postProcessBeanFactory(this.beanFactory);
		TaskExecutorJobOperator jobOperator = new TaskExecutorJobOperator();

		// when
		this.postProcessor.postProcessAfterInitialization(jobOperator, "jobOperator");

		// then
		assertSame(this.observationRegistry, ReflectionTestUtils.getField(jobOperator, "observationRegistry"));
		assertSame(this.batchEventRecorder, ReflectionTestUtils.getField(jobOperator, "batchEventRecorder"));
	}

	@Test
	void observabilityComponentsShouldBeSetOnProxiedJobOperator() {
		// given
		this.beanFactory.registerSingleton("observationRegistry", this.observationRegistry);
		this.beanFactory.registerSingleton("batchEventRecorder", this.batchEventRecorder);
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
		assertSame(this.batchEventRecorder, ReflectionTestUtils.getField(target, "batchEventRecorder"));
	}

}
