/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.execution.step;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.springframework.batch.core.configuration.StepConfigurationSupport;
import org.springframework.batch.core.executor.StepExecutor;
import org.springframework.batch.execution.step.simple.SimpleStepExecutor;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;

/**
 * @author Dave Syer
 *
 */
public class PrototypeBeanStepExecutorFactoryTests extends TestCase {

	private PrototypeBeanStepExecutorFactory factory = new PrototypeBeanStepExecutorFactory();
	private StaticApplicationContext applicationContext = new StaticApplicationContext();
	
	protected void setUp() throws Exception {
		factory.setBeanFactory(applicationContext);
	}
	
	public void testMissingStepExecutorName() throws Exception {
		try {
			factory.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		} catch(IllegalArgumentException e) {
			// Missing name is illegal
		}
	}

	public void testMissingStepExecutor() throws Exception {
		factory.setStepExecutorName("foo");
		try {
			factory.afterPropertiesSet();
			fail("Expected NoSuchBeanDefinitionException");
		} catch(NoSuchBeanDefinitionException e) {
			// expected
		}
	}
	
	public void testSingletonStepExecutor() throws Exception {
		applicationContext.getDefaultListableBeanFactory().registerBeanDefinition("foo", new RootBeanDefinition(SimpleStepExecutor.class));
		factory.setStepExecutorName("foo");
		try {
			factory.afterPropertiesSet();
			fail("Expected IllegalStateException");
		} catch(IllegalStateException e) {
			// expected
		}
	}

	public void testSuccessfulStepExecutor() throws Exception {
		SimpleStepExecutor executor = new SimpleStepExecutor();
		applicationContext.getBeanFactory().registerSingleton("foo", executor);
		factory.setStepExecutorName("foo");
		assertEquals(executor, factory.getExecutor(new SimpleStepConfiguration()));
	}

	public void testSuccessfulStepExecutorWithNonSimpleConfigugration() throws Exception {
		SimpleStepExecutor executor = new SimpleStepExecutor();
		applicationContext.getBeanFactory().registerSingleton("foo", executor);
		factory.setStepExecutorName("foo");
		assertEquals(executor, factory.getExecutor(new StepConfigurationSupport()));
	}

	public void testSuccessfulStepExecutorWithSimpleConfigurationAndNotSimpleExecutor() throws Exception {
		StepExecutor executor = new SimpleStepExecutor();
		applicationContext.getBeanFactory().registerSingleton("foo", executor);
		factory.setStepExecutorName("foo");
		assertEquals(executor, factory.getExecutor(new SimpleStepConfiguration()));
	}

	public void testSuccessfulStepExecutorHolderStrategy() throws Exception {
		SimpleStepExecutor executor = new SimpleStepExecutor();
		applicationContext.getBeanFactory().registerSingleton("foo", executor);
		factory.setStepExecutorName("foo");
		RepeatTemplate repeatTemplate = new RepeatTemplate();
		assertEquals(executor, factory.getExecutor(new SimpleHolderStepConfiguration(repeatTemplate)));
	}
	
	public void testSuccessfulStepExecutorHolderStrategyWithStepOperations() throws Exception {
		final List list = new ArrayList();
		SimpleStepExecutor executor = new SimpleStepExecutor() {
			public void setChunkOperations(RepeatOperations chunkOperations) {
				list.add(chunkOperations);
				super.setChunkOperations(chunkOperations);
			}
			public void setStepOperations(RepeatOperations stepOperations) {
				list.add(stepOperations);
				super.setStepOperations(stepOperations);
			}
		};
		applicationContext.getBeanFactory().registerSingleton("foo", executor);
		factory.setStepExecutorName("foo");
		RepeatTemplate chunkTemplate = new RepeatTemplate();
		RepeatTemplate stepTemplate = new RepeatTemplate();
		SimpleStepExecutor product = (SimpleStepExecutor) factory.getExecutor(new SimpleHolderStepConfiguration(chunkTemplate, stepTemplate));
		assertEquals(executor, product);
		assertEquals(2, list.size());
		assertEquals(chunkTemplate, list.get(0));
		assertEquals(stepTemplate, list.get(1));
	}

	public void testUnsuccessfulStepExecutorHolderStrategy() throws Exception {
		SimpleStepExecutor executor = new SimpleStepExecutor();
		applicationContext.getBeanFactory().registerSingleton("foo", executor);
		factory.setStepExecutorName("foo");
		try {
			factory.getExecutor(new SimpleHolderStepConfiguration(null));
			fail("Expected IllegalStateException");
		} catch (IllegalStateException e) {
			// expected
		}
	}

	/**
	 * @author Dave Syer
	 *
	 */
	public class SimpleHolderStepConfiguration extends SimpleStepConfiguration implements RepeatOperationsHolder {
		private RepeatOperations chunkOperations;
		private RepeatOperations stepOperations;
		public SimpleHolderStepConfiguration(RepeatOperations executor) {
			this.chunkOperations = executor;
		}
		public SimpleHolderStepConfiguration(RepeatOperations chunkOperations,
				RepeatOperations stepOperations) {
			this.chunkOperations = chunkOperations;
			this.stepOperations = stepOperations;
		}
		public RepeatOperations getChunkOperations() {
			return chunkOperations;
		}
		public RepeatOperations getStepOperations() {
			return stepOperations;
		}
	}

}
