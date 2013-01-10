/*
 * Copyright 2006-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Dave Syer
 * 
 */
public class StepScopeConfigurationTests {

	private ConfigurableApplicationContext context;

	private StepExecution stepExecution;

	@Rule
	public ExpectedException expected = ExpectedException.none();

	@Test
	public void testXmlStepScopeWithProxyTargetClass() throws Exception {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/annotation/StepScopeConfigurationTestsProxyTargetClass-context.xml");
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("STEP", value.call());
	}

	@Test
	public void testXmlStepScopeWithInterface() throws Exception {
		context = new ClassPathXmlApplicationContext(
				"org/springframework/batch/core/configuration/annotation/StepScopeConfigurationTestsInterface-context.xml");
		@SuppressWarnings("unchecked")
		Callable<String> value = context.getBean(Callable.class);
		assertEquals("STEP", value.call());
	}

	@Test
	public void testStepScopeWithProxyTargetClass() throws Exception {
		init(StepScopeConfigurationRequiringProxyTargetClass.class);
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("STEP", value.call());
	}

	@Test
	public void testIntentionallyBlowUpOnMissingContextWithProxyTargetClass() throws Exception {
		init(StepScopeConfigurationRequiringProxyTargetClass.class);
		StepSynchronizationManager.release();
		expected.expect(BeanCreationException.class);
		expected.expectMessage("step scope");
		SimpleHolder value = context.getBean(SimpleHolder.class);
		assertEquals("STEP", value.call());
	}

	@Test
	public void testStepScopeWithInterface() throws Exception {
		init(StepScopeConfigurationWithInterface.class);
		@SuppressWarnings("unchecked")
		Callable<String> value = context.getBean(Callable.class);
		assertEquals("STEP", value.call());
	}

	@Test
	public void testIntentionallyBlowUpOnMissingContextWithInterface() throws Exception {
		init(StepScopeConfigurationWithInterface.class);
		StepSynchronizationManager.release();
		expected.expect(BeanCreationException.class);
		expected.expectMessage("step scope");
		@SuppressWarnings("unchecked")
		Callable<String> value = context.getBean(Callable.class);
		assertEquals("STEP", value.call());
	}

	public void init(Class<?>... config) throws Exception {
		Class<?>[] configs = new Class<?>[config.length + 1];
		System.arraycopy(config, 0, configs, 1, config.length);
		configs[0] = DataSourceConfiguration.class;
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		// context.setScopeMetadataResolver(new AnnotationScopeMetadataResolver(ScopedProxyMode.TARGET_CLASS));
		context.register(configs);
		context.refresh();
		this.context = context;
	}

	@Before
	public void setup() {
		stepExecution = new StepExecution("STEP", null);
		StepSynchronizationManager.register(stepExecution);
	}

	@After
	public void close() {
		StepSynchronizationManager.release();
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

	@Configuration
	@EnableBatchProcessing
	public static class StepScopeConfigurationRequiringProxyTargetClass {

		@Bean
		@StepScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
		protected SimpleHolder value(@Value("#{stepExecution.stepName}")
		final String value) {
			return new SimpleHolder(value);
		}

	}

	@Configuration
	@EnableBatchProcessing
	public static class StepScopeConfigurationWithInterface {

		@Bean
		@StepScope
		protected Callable<String> value(@Value("#{stepExecution.stepName}")
		final String value) {
			return new SimpleCallable(value);
		}

	}

}
