/*
 * Copyright 2013-2022 the original author or authors.
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
package org.springframework.batch.core.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
class JobScopePlaceholderIntegrationTests implements BeanFactoryAware {

	@Autowired
	@Qualifier("simple")
	private Collaborator simple;

	@Autowired
	@Qualifier("compound")
	private Collaborator compound;

	@Autowired
	@Qualifier("value")
	private Collaborator value;

	@Autowired
	@Qualifier("ref")
	private Collaborator ref;

	@Autowired
	@Qualifier("scopedRef")
	private Collaborator scopedRef;

	@Autowired
	@Qualifier("list")
	private Collaborator list;

	@Autowired
	@Qualifier("bar")
	private Collaborator bar;

	@Autowired
	@Qualifier("nested")
	private Collaborator nested;

	private JobExecution jobExecution;

	private ListableBeanFactory beanFactory;

	private int beanCount;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@BeforeEach
	void start() {
		start("bar");
	}

	private void start(String foo) {

		JobSynchronizationManager.close();
		JobInstance jobInstance = new JobInstance(1L, "foo");
		jobExecution = new JobExecution(123L, jobInstance, new JobParameters());

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", foo);
		executionContext.put("parent", bar);

		jobExecution.setExecutionContext(executionContext);
		JobSynchronizationManager.register(jobExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@AfterEach
	void stop() {
		JobSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	void testSimpleProperty() {
		assertEquals("bar", simple.getName());
		// Once the job context is set up it should be baked into the proxies
		// so changing it now should have no effect
		jobExecution.getExecutionContext().put("foo", "wrong!");
		assertEquals("bar", simple.getName());
	}

	@Test
	void testCompoundProperty() {
		assertEquals("bar-bar", compound.getName());
	}

	@Test
	void testCompoundPropertyTwice() {

		assertEquals("bar-bar", compound.getName());

		JobSynchronizationManager.close();
		JobInstance jobInstance = new JobInstance(1L, "foo");
		jobExecution = new JobExecution(123L, jobInstance, new JobParameters());

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", "spam");

		jobExecution.setExecutionContext(executionContext);
		JobSynchronizationManager.register(jobExecution);

		assertEquals("spam-bar", compound.getName());

	}

	@Test
	void testParentByRef() {
		assertEquals("bar", ref.getParent().getName());
	}

	@Test
	void testParentByValue() {
		assertEquals("bar", value.getParent().getName());
	}

	@Test
	void testList() {
		assertEquals("[bar]", list.getList().toString());
	}

	@Test
	void testNested() {
		assertEquals("bar", nested.getParent().getName());
	}

	@Test
	void testScopedRef() {
		assertEquals("bar", scopedRef.getParent().getName());
		stop();
		start("spam");
		assertEquals("spam", scopedRef.getParent().getName());
	}

}
