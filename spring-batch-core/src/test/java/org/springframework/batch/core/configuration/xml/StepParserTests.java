/*
 * Copyright 2006-2009 the original author or authors.
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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;

import org.junit.Test;
import org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


/**
 * @author Thomas Risberg
 */
public class StepParserTests {
	
	@SuppressWarnings("unchecked")
	@Test
	public void testTaskletStepAttributes() throws Exception {
		ConfigurableApplicationContext ctx = 
			new ClassPathXmlApplicationContext("org/springframework/batch/core/configuration/xml/StepParserTaskletAttributesTests-context.xml");
		Map<String, Object> beans = ctx.getBeansOfType(FaultTolerantStepFactoryBean.class);
		String factoryName = (String) beans.keySet().toArray()[0];
		FaultTolerantStepFactoryBean<Object, Object> factory = (FaultTolerantStepFactoryBean<Object, Object>) beans.get(factoryName);
		TaskletStep bean = (TaskletStep) factory.getObject();
		assertEquals("wrong start-limit:", 25, bean.getStartLimit());
	}

	@Test
	public void testTaskletStepWithBadStepListener() throws Exception {
		loadContextWithBadListener("org/springframework/batch/core/configuration/xml/StepParserBadStepListenerTests-context.xml");
	}

	@Test
	public void testTaskletStepWithBadRetryListener() throws Exception {
		loadContextWithBadListener("org/springframework/batch/core/configuration/xml/StepParserBadRetryListenerTests-context.xml");
	}

	private void loadContextWithBadListener(String contextLocation) {
		try {
			new ClassPathXmlApplicationContext(contextLocation);
			fail("Context should not load!");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().contains("'ref' and 'class'"));
		}
	}

}
