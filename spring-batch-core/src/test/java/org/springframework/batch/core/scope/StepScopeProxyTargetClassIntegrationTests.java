/*
 * Copyright 2009-2022 the original author or authors.
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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
public class StepScopeProxyTargetClassIntegrationTests implements BeanFactoryAware {

	@Autowired
	@Qualifier("simple")
	private TestCollaborator simple;

	private StepExecution stepExecution;

	private ListableBeanFactory beanFactory;

	private int beanCount;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	@BeforeEach
	void start() {

		StepSynchronizationManager.close();
		stepExecution = new StepExecution("foo", new JobExecution(11L), 123L);

		ExecutionContext executionContext = new ExecutionContext();
		executionContext.put("foo", "bar");

		stepExecution.setExecutionContext(executionContext);
		StepSynchronizationManager.register(stepExecution);

		beanCount = beanFactory.getBeanDefinitionCount();

	}

	@AfterEach
	void cleanUp() {
		StepSynchronizationManager.close();
		// Check that all temporary bean definitions are cleaned up
		assertEquals(beanCount, beanFactory.getBeanDefinitionCount());
	}

	@Test
	void testSimpleProperty() {
		assertEquals("bar", simple.getName());
		// Once the step context is set up it should be baked into the proxies
		// so changing it now should have no effect
		stepExecution.getExecutionContext().put("foo", "wrong!");
		assertEquals("bar", simple.getName());
	}

}
