/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.core.configuration.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.StaticApplicationContext;

class ApplicationContextJobFactoryTests {

	@Test
	void testFactoryContext() {
		ApplicationContextJobFactory factory = new ApplicationContextJobFactory("job",
				new StubApplicationContextFactory());
		assertNotNull(factory.createJob());
	}

	@Test
	void testPostProcessing() {
		ApplicationContextJobFactory factory = new ApplicationContextJobFactory("job",
				new PostProcessingApplicationContextFactory());
		assertEquals("bar", factory.getJobName());
	}

	private static class StubApplicationContextFactory implements ApplicationContextFactory {

		@Override
		public ConfigurableApplicationContext createApplicationContext() {
			StaticApplicationContext context = new StaticApplicationContext();
			context.registerSingleton("job", JobSupport.class);
			return context;
		}

	}

	private static class PostProcessingApplicationContextFactory implements ApplicationContextFactory {

		@Override
		public ConfigurableApplicationContext createApplicationContext() {
			StaticApplicationContext context = new StaticApplicationContext();
			context.registerSingleton("job", JobSupport.class);
			context.registerSingleton("postProcessor", TestBeanPostProcessor.class);
			context.refresh();
			return context;
		}

	}

	private static class TestBeanPostProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof JobSupport) {
				((JobSupport) bean).setName("bar");
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			return bean;
		}

	}

}
