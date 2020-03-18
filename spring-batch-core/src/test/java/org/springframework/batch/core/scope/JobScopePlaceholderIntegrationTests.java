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
package org.springframework.batch.core.scope;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.scope.context.JobSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobScopePlaceholderIntegrationTests implements BeanFactoryAware {

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

	@Before
	public void start() {
		start("bar");
	}

	private void start(String foo) {

		JobSynchronizationManager.close();
		jobExecution = new JobExecution(123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", foo);
		executionContext.put("parent", bar);

		jobExecution.setExecutionContext(executionContext);
		JobSynchronizationManager.register(jobExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@After
	public void stop() {
		JobSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimpleProperty() throws Exception {
		assertEquals("bar", simple.getName());
		// Once the job context is set up it should be baked into the proxies
		// so changing it now should have no effect
		jobExecution.getExecutionContext().put("foo", "wrong!");
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testCompoundProperty() throws Exception {
		assertEquals("bar-bar", compound.getName());
	}

	@Test
	public void testCompoundPropertyTwice() throws Exception {

		assertEquals("bar-bar", compound.getName());

		JobSynchronizationManager.close();
		jobExecution = new JobExecution(123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", "spam");

		jobExecution.setExecutionContext(executionContext);
		JobSynchronizationManager.register(jobExecution);

		assertEquals("spam-bar", compound.getName());

	}

	@Test
	public void testParentByRef() throws Exception {
		assertEquals("bar", ref.getParent().getName());
	}

	@Test
	public void testParentByValue() throws Exception {
		assertEquals("bar", value.getParent().getName());
	}

	@Test
	public void testList() throws Exception {
		assertEquals("[bar]", list.getList().toString());
	}

	@Test
	public void testNested() throws Exception {
		assertEquals("bar", nested.getParent().getName());
	}

	@Test
	public void testScopedRef() throws Exception {
		assertEquals("bar", scopedRef.getParent().getName());
		stop();
		start("spam");
		assertEquals("spam", scopedRef.getParent().getName());
	}

}
