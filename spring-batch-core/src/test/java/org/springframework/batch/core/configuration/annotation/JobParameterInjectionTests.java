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

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.batch.infrastructure.support.DatabaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * @author Yanming Zhou
 * @author Mahmoud Ben Hassine
 */
class JobParameterInjectionTests {

	@ParameterizedTest
	@ValueSource(classes = { MethodParameterWithImplicitName.class, MethodParameterWithExplicitName.class,
			FieldWithImplicitName.class, FieldWithExplicitName.class, MethodParameterInJobScope.class })
	void testInjection(Class<?> configurationClass) throws Exception {
		// given
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configurationClass);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		LocalDate date = LocalDate.of(2018, 4, 25);
		JobExecution jobExecution = jobOperator.start(job, new JobParameters(Set
			.of(new org.springframework.batch.core.job.parameters.JobParameter<>("date", date, LocalDate.class))));

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertEquals(date, context.getBean(JobParameterHolder.class).getDate());
	}

	@Test
	void testMissingJobParameterIsInjectedAsNull() throws Exception {
		// given
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MethodParameterWithImplicitName.class);
		Job job = context.getBean(Job.class);
		JobOperator jobOperator = context.getBean(JobOperator.class);

		// when
		JobExecution jobExecution = jobOperator.start(job, JobParameters.EMPTY);

		// then
		Assertions.assertEquals(ExitStatus.COMPLETED, jobExecution.getExitStatus());
		Assertions.assertNull(context.getBean(JobParameterHolder.class).getDate());
	}

	@Test
	void testSuggestedValue() throws Exception {
		AutowireCandidateResolverHolder holder = new AutowireCandidateResolverHolder();

		Assertions.assertEquals("#{jobParameters['myName']}",
				holder.resolver.getSuggestedValue(descriptor("implicitName", 0)));
		Assertions.assertEquals("#{jobParameters['date']}",
				holder.resolver.getSuggestedValue(descriptor("explicitName", 0)));
	}

	@Test
	void testSuggestedValueDelegatesWhenNotAnnotated() throws Exception {
		AutowireCandidateResolverHolder holder = new AutowireCandidateResolverHolder();

		Assertions.assertNull(holder.resolver.getSuggestedValue(descriptor("notAnnotated", 0)));
	}

	@Test
	void testImplicitNameNotAvailable() throws Exception {
		AutowireCandidateResolverHolder holder = new AutowireCandidateResolverHolder();
		Method method = JobParameterInjectionTests.class.getDeclaredMethod("implicitName", String.class);
		MethodParameter parameterWithoutName = new MethodParameter(method, 0) {
			@Override
			public String getParameterName() {
				return null;
			}
		};

		IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
				() -> holder.resolver.getSuggestedValue(new DependencyDescriptor(parameterWithoutName, true)));
		Assertions.assertTrue(exception.getMessage().contains("implicitName"), exception.getMessage());
		Assertions.assertTrue(exception.getMessage().contains("-parameters"), exception.getMessage());
	}

	@Test
	void testCloneIfNecessaryKeepsJobParameterSupport() throws Exception {
		AutowireCandidateResolverHolder holder = new AutowireCandidateResolverHolder();
		DefaultListableBeanFactory copy = new DefaultListableBeanFactory();

		copy.copyConfigurationFrom(holder.beanFactory);

		Assertions.assertEquals("#{jobParameters['date']}",
				copy.getAutowireCandidateResolver().getSuggestedValue(descriptor("explicitName", 0)));
	}

	void implicitName(@JobParameter String myName) {
	}

	void explicitName(@JobParameter("date") String name) {
	}

	void notAnnotated(String name) {
	}

	private static DependencyDescriptor descriptor(String methodName, int parameterIndex) throws Exception {
		Method method = JobParameterInjectionTests.class.getDeclaredMethod(methodName, String.class);
		return new DependencyDescriptor(new MethodParameter(method, parameterIndex), true);
	}

	static class AutowireCandidateResolverHolder {

		final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();

		final org.springframework.beans.factory.support.AutowireCandidateResolver resolver;

		AutowireCandidateResolverHolder() {
			new LateBindingBeanFactoryPostProcessor().postProcessBeanFactory(this.beanFactory);
			this.resolver = this.beanFactory.getAutowireCandidateResolver();
		}

	}

	static class JobParameterInjectionConfiguration extends JdbcDefaultBatchConfiguration {

		@Bean
		Step myStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, Tasklet myTasklet) {
			return new StepBuilder("myStep", jobRepository).tasklet(myTasklet, transactionManager).build();
		}

		@Bean
		Job job(JobRepository jobRepository, Step myStep) {
			return new JobBuilder("job", jobRepository).start(myStep).build();
		}

		@Bean
		JobParameterHolder jobParameterHolder() {
			return new JobParameterHolder();
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.HSQL)
				.addScript(DatabaseType.HSQL.getProductSchema())
				.generateUniqueName(true)
				.build();
		}

		@Bean
		PlatformTransactionManager transactionManager(DataSource dataSource) {
			return new JdbcTransactionManager(dataSource);
		}

	}

	@Configuration
	static class MethodParameterWithImplicitName extends JobParameterInjectionConfiguration {

		@Bean
		@StepScope
		public Tasklet myTasklet(JobParameterHolder jobParameterHolder, @JobParameter LocalDate date) {
			return (contribution, chunkContext) -> {
				jobParameterHolder.setDate(date);
				return RepeatStatus.FINISHED;
			};
		}

	}

	@Configuration
	static class MethodParameterWithExplicitName extends JobParameterInjectionConfiguration {

		@Bean
		@StepScope
		public Tasklet myTasklet(JobParameterHolder jobParameterHolder, @JobParameter("date") LocalDate myDate) {
			return (contribution, chunkContext) -> {
				jobParameterHolder.setDate(myDate);
				return RepeatStatus.FINISHED;
			};
		}

	}

	@Configuration
	static class MethodParameterInJobScope extends JobParameterInjectionConfiguration {

		@Bean
		@JobScope
		public Tasklet myTasklet(JobParameterHolder jobParameterHolder, @JobParameter LocalDate date) {
			return (contribution, chunkContext) -> {
				jobParameterHolder.setDate(date);
				return RepeatStatus.FINISHED;
			};
		}

	}

	@Configuration
	static class FieldWithImplicitName extends JobParameterInjectionConfiguration {

		@Bean
		@StepScope
		public Tasklet myTasklet() {
			return new MyTaskletWithImplicitJobParameterName();
		}

	}

	@Configuration
	static class FieldWithExplicitName extends JobParameterInjectionConfiguration {

		@Bean
		@StepScope
		public Tasklet myTasklet() {
			return new MyTaskletWithExplicitJobParameterName();
		}

	}

	static class JobParameterHolder {

		private LocalDate date = LocalDate.now();

		public LocalDate getDate() {
			return date;
		}

		public void setDate(LocalDate date) {
			this.date = date;
		}

	}

	static class MyTaskletWithImplicitJobParameterName implements Tasklet {

		@Autowired
		JobParameterHolder jobParameterHolder;

		@JobParameter
		LocalDate date;

		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			jobParameterHolder.setDate(date);
			return RepeatStatus.FINISHED;
		}

	}

	static class MyTaskletWithExplicitJobParameterName implements Tasklet {

		@Autowired
		JobParameterHolder jobParameterHolder;

		@JobParameter("date")
		LocalDate myDate;

		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			jobParameterHolder.setDate(myDate);
			return RepeatStatus.FINISHED;
		}

	}

}
