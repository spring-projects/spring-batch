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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration()
@RunWith(SpringJUnit4ClassRunner.class)
public class SplitParsingTests {
	@Autowired
	public Job job;

	@Autowired
	public JobLauncher jobLauncher;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void test() throws Exception {
		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(5, execution.getStepExecutions().size());
	}

	@Test
	public void testOneFlowInSplit() {
		try {
			new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/invalid-split-context.xml");
		} catch (BeanDefinitionParsingException bdpe) {
			assertTrue(bdpe.getMessage().contains("A <split/> must contain at least two 'flow' elements."));
			return;
		}

		fail("Expected exception was not thrown");
	}

	@Test
	public void testUserSpecifiedTaskExecutor() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/user-specified-split-task-executor-context.xml");
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();
		PropertyValue propertyValue = new SplitParser(null).getSplitTaskExecutorPropertyValue(registry);

		RuntimeBeanReference runtimeBeanReferenceValue = (RuntimeBeanReference) propertyValue.getValue();

		Assert.assertTrue("RuntimeBeanReference should have a name of jsr352splitTaskExecutor" , "jsr352splitTaskExecutor".equals(runtimeBeanReferenceValue.getBeanName()));
		context.close();
	}

	@Test
	public void testDefaultTaskExecutor() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("/org/springframework/batch/core/jsr/configuration/xml/default-split-task-executor-context.xml");
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) context.getBeanFactory();
		PropertyValue propertyValue = new SplitParser(null).getSplitTaskExecutorPropertyValue(registry);
		Assert.assertTrue("Task executor not an instance of SimpleAsyncTaskExecutor" , (propertyValue.getValue() instanceof SimpleAsyncTaskExecutor));
		context.close();
	}
}
