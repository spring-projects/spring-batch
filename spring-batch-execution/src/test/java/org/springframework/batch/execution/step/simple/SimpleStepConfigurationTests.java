/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.execution.step.simple;

import junit.framework.TestCase;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.item.reader.AbstractItemReader;
import org.springframework.batch.item.writer.AbstractItemWriter;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.exception.handler.DefaultExceptionHandler;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;

/**
 * @author Dave Syer
 * 
 */
public class SimpleStepConfigurationTests extends TestCase {

	SimpleStep configuration = new SimpleStep("foo");

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.SimpleStep#SimpleStepConfiguration()}.
	 */
	public void testSimpleStepConfiguration() {
		assertNotNull(configuration.getName());
		configuration = new SimpleStep();
		assertNull(configuration.getName());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.execution.step.simple.SimpleStep#SimpleStepConfiguration(org.springframework.batch.core.tasklet.Tasklet)}.
	 * 
	 * @throws Exception
	 */
	public void testSimpleStepConfigurationTasklet() throws Exception {
		configuration = new SimpleStep();
		configuration.setItemReader(new AbstractItemReader() {

			public Object read() throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
		});
		configuration.setItemWriter(new AbstractItemWriter() {

			public void write(Object item) throws Exception {
	            // TODO Auto-generated method stub
	            
            }
		});
		configuration.setJobRepository(new JobRepositorySupport());
		configuration.setTransactionManager(new ResourcelessTransactionManager());
		configuration.afterPropertiesSet();
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.SimpleStep#getCommitInterval()}.
	 */
	public void testGetCommitInterval() {
		assertEquals(1, configuration.getCommitInterval());
		configuration.setCommitInterval(20);
		assertEquals(20, configuration.getCommitInterval());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.AbstractStep#getExceptionHandler()}.
	 */
	public void testGetExceptionHandler() {
		assertNull(configuration.getExceptionHandler());
		configuration.setExceptionHandler(new DefaultExceptionHandler());
		assertNotNull(configuration.getExceptionHandler());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.AbstractStep#getExceptionHandler()}.
	 */
	public void testSkipLimit() {
		assertEquals(0, configuration.getSkipLimit());
		configuration.setSkipLimit(2);
		assertEquals(2, configuration.getSkipLimit());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.AbstractStep#getSkipLimit()}.
	 */
	public void testGetSkipLimit() {
		assertEquals(0, configuration.getSkipLimit());
		configuration.setSkipLimit(20);
		assertEquals(20, configuration.getSkipLimit());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.simple.AbstractStep#isSaveExecutionAttributes()}.
	 */
	public void testIsSaveExecutionAttributes() {
		assertEquals(false, configuration.isSaveExecutionAttributes());
		configuration.setSaveExecutionAttributes(true);
		assertEquals(true, configuration.isSaveExecutionAttributes());
	}

}
