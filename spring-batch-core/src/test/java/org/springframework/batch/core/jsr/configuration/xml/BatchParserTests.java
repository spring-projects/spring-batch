/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.batch.core.PooledEmbeddedDataSource;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.xml.DummyItemProcessor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

public class BatchParserTests {

	private ApplicationContext baseContext;

	@Before
	public void setUp() {
		baseContext = new AnnotationConfigApplicationContext(BaseConfiguration.class);
	}

	@Test
	@Ignore
	public void testRoseyScenario() {
		GenericXmlApplicationContext batchContext = new GenericXmlApplicationContext();
		batchContext.setValidating(false);
		batchContext.load(new String[] {"classpath:/org/springframework/batch/core/jsr/configuration/xml/batch.xml"});
		System.out.println("baseContext = " + baseContext);
		batchContext.setParent(baseContext);
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(AutowiredAnnotationBeanPostProcessor.class);
		batchContext.registerBeanDefinition("postProcessor", bd);
		batchContext.refresh();

		Object itemProcessor = batchContext.getBean(ItemProcessor.class);

		assertNotNull(itemProcessor);
		assertTrue(itemProcessor instanceof PassThroughItemProcessor);

		batchContext.close();
	}

	@Test
	@Ignore
	@SuppressWarnings({"resource", "rawtypes"})
	public void testOverrideBeansFirst() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/override_batch.xml",
				"/org/springframework/batch/core/jsr/configuration/xml/batch.xml");

		ItemProcessor processor = (ItemProcessor) context.getBean("itemProcessor");

		assertNotNull(processor);
		assertTrue(processor instanceof DummyItemProcessor);
	}

	@Test
	@Ignore
	@SuppressWarnings({"resource", "rawtypes"})
	public void testOverrideBeansLast() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/batch.xml",
				"/org/springframework/batch/core/jsr/configuration/xml/override_batch.xml");

		ItemProcessor processor = (ItemProcessor) context.getBean("itemProcessor");

		assertNotNull(processor);
		assertTrue(processor instanceof DummyItemProcessor);
	}

	@Configuration
	@EnableBatchProcessing
	public static class BaseConfiguration extends DefaultBatchConfigurer {

		@Bean
		DataSource dataSource() {
			return new PooledEmbeddedDataSource(new EmbeddedDatabaseBuilder().
					addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
					addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
					build());
		}
	}
}
