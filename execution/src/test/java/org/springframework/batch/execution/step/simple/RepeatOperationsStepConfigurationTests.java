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

import org.springframework.batch.execution.step.RepeatOperationsStepConfiguration;
import org.springframework.batch.repeat.support.RepeatTemplate;

/**
 * @author Dave Syer
 *
 */
public class RepeatOperationsStepConfigurationTests extends TestCase {

	RepeatOperationsStepConfiguration configuration = new RepeatOperationsStepConfiguration();
	
	/**
	 * Test method for {@link org.springframework.batch.execution.step.RepeatOperationsStepConfiguration#getChunkOperations()}.
	 */
	public void testSetChunkOperations() {
		assertNull(configuration.getChunkOperations());
		RepeatTemplate executor = new RepeatTemplate();
		configuration.setChunkOperations(executor);
		assertEquals(executor, configuration.getChunkOperations());
		
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.RepeatOperationsStepConfiguration#getChunkOperations()}.
	 */
	public void testSetStepOperations() {
		assertNull(configuration.getChunkOperations());
		RepeatTemplate executor = new RepeatTemplate();
		configuration.setStepOperations(executor);
		assertEquals(executor, configuration.getStepOperations());
		
	}
}
