/*
 * Copyright 2006-2019 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Callable;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 */
public class JobScopeConfigurationTests {

	private ConfigurableApplicationContext context;

	private JobExecution jobExecution;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testXmlJobScopeWithProxyTargetClass() throws Exception {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/annotation/JobScopeConfigurationTestsProxyTargetClass-context.xml");
		JobSynchronizationManager.register(jobExecution);
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("JOB", value.call());
	}

	@Test
	public void testXmlJobScopeWithInterface() throws Exception {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/annotation/JobScopeConfigurationTestsInterface-context.xml");
		JobSynchronizationManager.register(jobExecution);
		@SuppressWarnings("unchecked")
		Callable<String> value = context.getBean(Callable.class);
		assertEquals("JOB", value.call());
	}

	@Test
	public void testXmlJobScopeWithInheritance() throws Exception {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/annotation/JobScopeConfigurationTestsInheritance-context.xml");
		JobSynchronizationManager.register(jobExecution);
		SimpleHolder value = (SimpleHolder) context.getBean("child");
		assertEquals("JOB", value.call());
	}

	@Test
	public void testJobScopeWithProxyTargetClass() throws Exception {
		init(JobScopeConfigurationRequiringProxyTargetClass.class);
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("JOB", value.call());
	}

	@Test
	public void testStepScopeXmlImportUsingNamespace() throws Exception {
		init(JobScopeConfigurationXmlImportUsingNamespace.class);

		SimpleHolder value = (SimpleHolder) context.getBean("xmlValue");
		assertEquals("JOB", value.call());
		value = (SimpleHolder) context.getBean("javaValue");
		assertEquals("JOB", value.call());
	}

	@Test
	public void testJobScopeWithProxyTargetClassInjected() throws Exception {
		init(JobScopeConfigurationInjectingProxy.class);
		SimpleHolder value = context.getBean(Wrapper.class).getValue();
		assertEquals("JOB", value.call());
	}

	@Test
	public void testIntentionallyBlowUpOnMissingContextWithProxyTargetClass() throws Exception {
		init(JobScopeConfigurationRequiringProxyTargetClass.class);
		JobSynchronizationManager.release();
		expected.expect(BeanCreationException.class);
		expected.expectMessage("job scope");
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("JOB", value.call());
	}

	@Test
	public void testIntentionallyBlowupWithForcedInterface() throws Exception {
		init(JobScopeConfigurationForcingInterfaceProxy.class);
		JobSynchronizationManager.release();
		expected.expect(BeanCreationException.class);
		expected.expectMessage("job scope");
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("JOB", value.call());
	}

	@Test
	public void testJobScopeWithDefaults() throws Exception {
		init(JobScopeConfigurationWithDefaults.class);
		@SuppressWarnings("unchecked")
		Callable<String> value = context.getBean(Callable.class);
		assertEquals("JOB", value.call());
	}

	@Test
	public void testIntentionallyBlowUpOnMissingContextWithInterface() throws Exception {
		init(JobScopeConfigurationWithDefaults.class);
		JobSynchronizationManager.release();
		expected.expect(BeanCreationException.class);
		expected.expectMessage("job scope");
		@SuppressWarnings("unchecked")
		Callable<String> value = context.getBean(Callable.class);
		assertEquals("JOB", value.call());
	}

	public void init(Class<?>... config) throws Exception {
		Class<?>[] configs = new Class<?>[config.length + 1];
		System.arraycopy(config, 0, configs, 1, config.length);
		configs[0] = DataSourceConfiguration.class;
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(configs);
		context.refresh();
		this.context = context;
		JobSynchronizationManager.register(jobExecution);
	}

	@Before
	public void setup() {
		JobSynchronizationManager.release();
		jobExecution = new JobExecution(new JobInstance(5l, "JOB"), null, null);
	}

	@After
	public void close() {
		JobSynchronizationManager.release();
		if (context != null) {
			context.close();
		}
	}

	public static class SimpleCallable implements Callable<String> {
		private final String value;

		private SimpleCallable(String value) {
			this.value = value;
		}

		@Override
		public String call() throws Exception {
			return value;
		}
	}

	public static class SimpleHolder {
		private final String value;

		protected SimpleHolder() {
			value = "<WRONG>";
		}

		public SimpleHolder(String value) {
			this.value = value;
		}

		public String call() throws Exception {
			return value;
		}
	}

	public static class Wrapper {

		private SimpleHolder value;

		public Wrapper(SimpleHolder value) {
			this.value = value;
		}

		public SimpleHolder getValue() {
			return value;
		}

	}

	public static class TaskletSupport implements Tasklet {

		@Nullable
		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			return RepeatStatus.FINISHED;
		}
	}

	@Configuration
	@ImportResource("org/springframework/batch/core/configuration/annotation/JobScopeConfigurationTestsXmlImportUsingNamespace-context.xml")
	@EnableBatchProcessing
	public static class JobScopeConfigurationXmlImportUsingNamespace {

		@Bean
		@JobScope
		protected SimpleHolder javaValue(@Value("#{jobName}") final String value) {
			return new SimpleHolder(value);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobScopeConfigurationInjectingProxy {

		@Bean
		public Wrapper wrapper(SimpleHolder value) {
			return new Wrapper(value);
		}

		@Bean
		@Scope(value="job", proxyMode = ScopedProxyMode.TARGET_CLASS)
		protected SimpleHolder value(@Value("#{jobName}")
		final String value) {
			return new SimpleHolder(value);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobScopeConfigurationRequiringProxyTargetClass {

		@Bean
		@Scope(value="job", proxyMode = ScopedProxyMode.TARGET_CLASS)
		protected SimpleHolder value(@Value("#{jobName}")
		final String value) {
			return new SimpleHolder(value);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobScopeConfigurationWithDefaults {

		@Bean
		@JobScope
		protected Callable<String> value(@Value("#{jobName}")
		final String value) {
			return new SimpleCallable(value);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class JobScopeConfigurationForcingInterfaceProxy {

		@Bean
		@Scope(value="job", proxyMode = ScopedProxyMode.INTERFACES)
		protected SimpleHolder value(@Value("#{jobName}")
		final String value) {
			return new SimpleHolder(value);
		}

	}

}
