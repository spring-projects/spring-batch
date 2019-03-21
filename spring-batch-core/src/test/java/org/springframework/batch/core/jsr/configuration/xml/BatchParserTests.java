/*
 * Copyright 2013 the original author or authors.
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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.xml.DummyItemProcessor;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class BatchParserTests {

	@Test
	@SuppressWarnings("unchecked")
	public void testRoseyScenario() throws Exception {
		JsrXmlApplicationContext context = new JsrXmlApplicationContext();
		Resource batchXml = new ClassPathResource("/org/springframework/batch/core/jsr/configuration/xml/batch.xml");
		context.setValidating(false);
		context.load(batchXml);

		GenericBeanDefinition stepScope = new GenericBeanDefinition();
		stepScope.setBeanClass(StepScope.class);
		context.registerBeanDefinition("stepScope", stepScope);

		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(AutowiredAnnotationBeanPostProcessor.class);
		context.registerBeanDefinition("postProcessor", bd);
		context.refresh();

		ItemProcessor<String, String> itemProcessor = context.getBean(ItemProcessor.class);

		assertNotNull(itemProcessor);
		StepSynchronizationManager.register(new StepExecution("step1", new JobExecution(5l)));
		assertEquals("Test", itemProcessor.process("Test"));
		StepSynchronizationManager.close();

		context.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOverrideBeansFirst() throws Exception {
		JsrXmlApplicationContext context = new JsrXmlApplicationContext();
		Resource overrideXml = new ClassPathResource("/org/springframework/batch/core/jsr/configuration/xml/override_batch.xml");
		Resource batchXml = new ClassPathResource("/org/springframework/batch/core/jsr/configuration/xml/batch.xml");

		context.setValidating(false);
		context.load(overrideXml, batchXml);
		context.refresh();

		ItemProcessor<String, String> itemProcessor = context.getBean("itemProcessor", ItemProcessor.class);

		assertNotNull(itemProcessor);
		StepSynchronizationManager.register(new StepExecution("step1", new JobExecution(5l)));
		assertEquals("Test", itemProcessor.process("Test"));
		StepSynchronizationManager.close();

		context.close();
	}

	@Test
	@SuppressWarnings({"resource", "rawtypes"})
	public void testOverrideBeansLast() {
		JsrXmlApplicationContext context = new JsrXmlApplicationContext();
		Resource overrideXml = new ClassPathResource("/org/springframework/batch/core/jsr/configuration/xml/override_batch.xml");
		Resource batchXml = new ClassPathResource("/org/springframework/batch/core/jsr/configuration/xml/batch.xml");

		context.setValidating(false);
		context.load(batchXml, overrideXml);
		context.refresh();

		ItemProcessor processor = (ItemProcessor) context.getBean("itemProcessor");

		assertNotNull(processor);
		assertTrue(processor instanceof DummyItemProcessor);
		context.close();
	}
}
