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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
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
public class StepScopeProxyTargetClassOverrideIntegrationTests implements BeanFactoryAware {
	
	private static final String JDK_PROXY_TO_STRING_REGEX = "class .*\\$Proxy\\d+";	
	
	private static final String CGLIB_PROXY_TO_STRING_REGEX = "class .*\\$EnhancerBySpringCGLIB.*";

	@Autowired
	@Qualifier("simple")
	private TestCollaborator simple;

	@Autowired
	@Qualifier("simpleProxyTargetClassTrue")
	private TestCollaborator simpleProxyTargetClassTrue;

	@Autowired
	@Qualifier("simpleProxyTargetClassFalse")
	private Collaborator simpleProxyTargetClassFalse;
	
	@Autowired
	@Qualifier("nested")
	private Step nested;

	@Autowired
	@Qualifier("nestedProxyTargetClassTrue")
	private Step nestedProxyTargetClassTrue;

	@Autowired
	@Qualifier("nestedProxyTargetClassFalse")
	private Step nestedProxyTargetClassFalse;

	private StepExecution stepExecution;

	private ListableBeanFactory beanFactory;

	private int beanCount;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@Before
	public void start() {

		StepSynchronizationManager.close();
		TestStep.reset();
		stepExecution = new StepExecution("foo", new JobExecution(11L), 123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", "bar");

		stepExecution.setExecutionContext(executionContext);
		StepSynchronizationManager.register(stepExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@After
	public void cleanUp() {
		StepSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	public void testSimple() throws Exception {
		assertTrue(AopUtils.isCglibProxy(simple));
		assertEquals("bar", simple.getName());
	}

	@Test
	public void testSimpleProxyTargetClassTrue() throws Exception {
		assertTrue(AopUtils.isCglibProxy(simpleProxyTargetClassTrue));
		assertEquals("bar", simpleProxyTargetClassTrue.getName());
	}
	
	@Test
	public void testSimpleProxyTargetClassFalse() throws Exception {
		assertTrue(AopUtils.isJdkDynamicProxy(simpleProxyTargetClassFalse));
		assertEquals("bar", simpleProxyTargetClassFalse.getName());
	}
	
	@Test
	public void testNested() throws Exception {
		nested.execute(new StepExecution("foo", new JobExecution(11L), 31L));
		assertTrue(TestStep.getContext().attributeNames().length > 0);
		String collaborator = (String) TestStep.getContext().getAttribute("collaborator");
		assertNotNull(collaborator);
		assertEquals("foo", collaborator);
		String parent = (String) TestStep.getContext().getAttribute("parent");
		assertNotNull(parent);
		assertEquals("bar", parent);
		assertTrue("Scoped proxy not created", ((String) TestStep.getContext().getAttribute("parent.class"))
				.matches(CGLIB_PROXY_TO_STRING_REGEX));
	}
	
	@Test
	public void testNestedProxyTargetClassTrue() throws Exception {
		nestedProxyTargetClassTrue.execute(new StepExecution("foo", new JobExecution(11L), 31L));
		String parent = (String) TestStep.getContext().getAttribute("parent");
		assertEquals("bar", parent);
		assertTrue("Scoped proxy not created", ((String) TestStep.getContext().getAttribute("parent.class"))
				.matches(CGLIB_PROXY_TO_STRING_REGEX));
	}	

	@Test
	public void testNestedProxyTargetClassFalse() throws Exception {
		nestedProxyTargetClassFalse.execute(new StepExecution("foo", new JobExecution(11L), 31L));
		String parent = (String) TestStep.getContext().getAttribute("parent");
		assertEquals("bar", parent);
		assertTrue("Scoped proxy not created", ((String) TestStep.getContext().getAttribute("parent.class"))
				.matches(JDK_PROXY_TO_STRING_REGEX));
	}	

}
