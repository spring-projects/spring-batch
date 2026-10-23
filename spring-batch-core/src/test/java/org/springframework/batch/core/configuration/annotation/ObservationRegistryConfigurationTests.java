/*
 * Copyright 2026-present the original author or authors.
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

import org.springframework.aop.support.AopUtils;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.AopTestUtils;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests that an {@link ObservationRegistry} bean of the application context does not
 * override an observation registry configured explicitly on a job, a step or a job
 * operator.
 */
class ObservationRegistryConfigurationTests {

	private static final ObservationRegistry GLOBAL_OBSERVATION_REGISTRY = ObservationRegistry.create();

	private static final ObservationRegistry CUSTOM_JOB_OBSERVATION_REGISTRY = ObservationRegistry.create();

	private static final ObservationRegistry CUSTOM_STEP_OBSERVATION_REGISTRY = ObservationRegistry.create();

	@Test
	void customObservationRegistryOfJobsAndStepsShouldBeRespected() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EnableBatchProcessingConfiguration.class)) {
			assertSame(CUSTOM_JOB_OBSERVATION_REGISTRY,
					context.getBean("customJob", AbstractJob.class).getObservationRegistry());
			assertSame(CUSTOM_STEP_OBSERVATION_REGISTRY,
					context.getBean("customStep", AbstractStep.class).getObservationRegistry());
		}
	}

	@Test
	void explicitNoopObservationRegistryOfJobsAndStepsShouldBeRespected() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EnableBatchProcessingConfiguration.class)) {
			assertSame(ObservationRegistry.NOOP,
					context.getBean("noopJob", AbstractJob.class).getObservationRegistry());
			assertSame(ObservationRegistry.NOOP,
					context.getBean("noopStep", AbstractStep.class).getObservationRegistry());
		}
	}

	@Test
	void observationRegistryBeanShouldBeSetOnJobsAndStepsWithoutObservationRegistry() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EnableBatchProcessingConfiguration.class)) {
			assertSame(GLOBAL_OBSERVATION_REGISTRY,
					context.getBean("defaultJob", AbstractJob.class).getObservationRegistry());
			assertSame(GLOBAL_OBSERVATION_REGISTRY,
					context.getBean("defaultStep", AbstractStep.class).getObservationRegistry());
		}
	}

	@Test
	void observationRegistryBeanShouldBeSetOnJobOperatorOfEnableBatchProcessing() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				EnableBatchProcessingConfiguration.class)) {
			assertSame(GLOBAL_OBSERVATION_REGISTRY, jobOperator(context).getObservationRegistry());
		}
	}

	@Test
	void observationRegistryBeanShouldBeSetOnJobOperatorOfDefaultBatchConfiguration() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DefaultBatchConfigurationWithObservationRegistry.class)) {
			assertSame(GLOBAL_OBSERVATION_REGISTRY, jobOperator(context).getObservationRegistry());
		}
	}

	@Test
	void noopObservationRegistryShouldBeSetOnJobOperatorOfDefaultBatchConfigurationWithoutObservationRegistryBean() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DefaultBatchConfiguration.class)) {
			assertSame(ObservationRegistry.NOOP, jobOperator(context).getObservationRegistry());
		}
	}

	@Test
	void explicitObservationRegistryOfDefaultBatchConfigurationShouldBeRespected() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DefaultBatchConfigurationWithExplicitNoop.class)) {
			assertSame(ObservationRegistry.NOOP, jobOperator(context).getObservationRegistry());
		}
	}

	@Test
	void noopObservationRegistryShouldBeSetOnJobOperatorOfDefaultBatchConfigurationWithSeveralObservationRegistryBeans() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DefaultBatchConfigurationWithSeveralObservationRegistries.class)) {
			assertSame(ObservationRegistry.NOOP, jobOperator(context).getObservationRegistry());
		}
	}

	@Test
	void primaryObservationRegistryShouldBeSetOnJobOperatorOfDefaultBatchConfigurationWithSeveralObservationRegistryBeans() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				DefaultBatchConfigurationWithPrimaryObservationRegistry.class)) {
			assertSame(GLOBAL_OBSERVATION_REGISTRY, jobOperator(context).getObservationRegistry());
		}
	}

	private static TaskExecutorJobOperator jobOperator(AnnotationConfigApplicationContext context) {
		JobOperator jobOperator = context.getBean(JobOperator.class);
		return (TaskExecutorJobOperator) (AopUtils.isAopProxy(jobOperator)
				? AopTestUtils.getUltimateTargetObject(jobOperator) : jobOperator);
	}

	@Configuration
	@EnableBatchProcessing
	static class EnableBatchProcessingConfiguration {

		@Bean
		ObservationRegistry observationRegistry() {
			return GLOBAL_OBSERVATION_REGISTRY;
		}

		@Bean
		Job customJob(JobRepository jobRepository) {
			return new JobBuilder("customJob", jobRepository).observationRegistry(CUSTOM_JOB_OBSERVATION_REGISTRY)
				.start(customStep(jobRepository))
				.build();
		}

		@Bean
		Step customStep(JobRepository jobRepository) {
			return new StepBuilder("customStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, new ResourcelessTransactionManager())
				.observationRegistry(CUSTOM_STEP_OBSERVATION_REGISTRY)
				.build();
		}

		@Bean
		Job noopJob(JobRepository jobRepository) {
			return new JobBuilder("noopJob", jobRepository).observationRegistry(ObservationRegistry.NOOP)
				.start(noopStep(jobRepository))
				.build();
		}

		@Bean
		Step noopStep(JobRepository jobRepository) {
			return new StepBuilder("noopStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, new ResourcelessTransactionManager())
				.observationRegistry(ObservationRegistry.NOOP)
				.build();
		}

		@Bean
		Job defaultJob(JobRepository jobRepository) {
			return new JobBuilder("defaultJob", jobRepository).start(defaultStep(jobRepository)).build();
		}

		@Bean
		Step defaultStep(JobRepository jobRepository) {
			return new StepBuilder("defaultStep", jobRepository)
				.tasklet((contribution, chunkContext) -> RepeatStatus.FINISHED, new ResourcelessTransactionManager())
				.build();
		}

	}

	@Configuration
	static class DefaultBatchConfigurationWithObservationRegistry extends DefaultBatchConfiguration {

		@Bean
		ObservationRegistry observationRegistry() {
			return GLOBAL_OBSERVATION_REGISTRY;
		}

	}

	@Configuration
	static class DefaultBatchConfigurationWithSeveralObservationRegistries extends DefaultBatchConfiguration {

		@Bean
		ObservationRegistry firstObservationRegistry() {
			return GLOBAL_OBSERVATION_REGISTRY;
		}

		@Bean
		ObservationRegistry secondObservationRegistry() {
			return ObservationRegistry.create();
		}

	}

	@Configuration
	static class DefaultBatchConfigurationWithPrimaryObservationRegistry extends DefaultBatchConfiguration {

		@Bean
		@Primary
		ObservationRegistry primaryObservationRegistry() {
			return GLOBAL_OBSERVATION_REGISTRY;
		}

		@Bean
		ObservationRegistry otherObservationRegistry() {
			return ObservationRegistry.create();
		}

	}

	@Configuration
	static class DefaultBatchConfigurationWithExplicitNoop extends DefaultBatchConfiguration {

		@Bean
		ObservationRegistry observationRegistry() {
			return GLOBAL_OBSERVATION_REGISTRY;
		}

		@Override
		protected ObservationRegistry getObservationRegistry() {
			return ObservationRegistry.NOOP;
		}

	}

}
