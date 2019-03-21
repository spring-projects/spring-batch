/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import org.junit.Test;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.support.GenericApplicationContext;
import static org.junit.Assert.assertEquals;

/**
 * <p>
 * Test cases around scoping of job/step listeners when building their bean definitions.
 * </p>
 *
 * @author Chris Schaefer
 */
public class ListenerParserTests {
	@Test
	public void testStepListenerStepScoped() {
		@SuppressWarnings("resource")
		GenericApplicationContext applicationContext = new GenericApplicationContext();

		AbstractBeanDefinition newBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition("stepListener").getBeanDefinition();
		newBeanDefinition.setScope("step");

		applicationContext.registerBeanDefinition("stepListener", newBeanDefinition);

		ListenerParser listenerParser = new ListenerParser(StepListenerFactoryBean.class, "listeners");
		listenerParser.applyListenerScope("stepListener", applicationContext);

		BeanDefinition beanDefinition = applicationContext.getBeanDefinition("stepListener");
		assertEquals("step", beanDefinition.getScope());
	}

	@Test
	public void testJobListenerSingletonScoped() {
		@SuppressWarnings("resource")
		GenericApplicationContext applicationContext = new GenericApplicationContext();

		AbstractBeanDefinition newBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition("jobListener").getBeanDefinition();
		newBeanDefinition.setScope("step");

		applicationContext.registerBeanDefinition("jobListener", newBeanDefinition);

		ListenerParser listenerParser = new ListenerParser(JsrJobListenerFactoryBean.class, "jobExecutionListeners");
		listenerParser.applyListenerScope("jobListener", applicationContext);

		BeanDefinition beanDefinition = applicationContext.getBeanDefinition("jobListener");
		assertEquals("job", beanDefinition.getScope());
	}
}
