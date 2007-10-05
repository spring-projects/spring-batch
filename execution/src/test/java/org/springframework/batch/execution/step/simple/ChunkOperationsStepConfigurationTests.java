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
import org.springframework.batch.execution.step.ChunkOperationsStepConfiguration;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.support.RepeatTemplate;

/**
 * @author Dave Syer
 *
 */
public class ChunkOperationsStepConfigurationTests extends TestCase {

	ChunkOperationsStepConfiguration configuration = new ChunkOperationsStepConfiguration();
	
	/**
	 * Test method for {@link org.springframework.batch.execution.step.ChunkOperationsStepConfiguration#StepExecutorStepConfiguration(org.springframework.batch.core.executor.StepExecutor)}.
	 */
	public void testStepExecutorStepConfigurationRepeatOperations() {
		RepeatTemplate executor = new RepeatTemplate();
		configuration = new ChunkOperationsStepConfiguration(executor);
		assertEquals(executor, configuration.getChunkOperations());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.ChunkOperationsStepConfiguration#StepExecutorStepConfiguration(org.springframework.batch.core.tasklet.Tasklet)}.
	 */
	public void testStepExecutorStepConfigurationTasklet() {
		Tasklet tasklet = new Tasklet() {
			public ExitStatus execute() throws Exception {
				return ExitStatus.FINISHED;
			}
		};
		configuration = new ChunkOperationsStepConfiguration(tasklet);
		assertEquals(tasklet, configuration.getTasklet());
	}

	/**
	 * Test method for {@link org.springframework.batch.execution.step.ChunkOperationsStepConfiguration#getChunkOperations()}.
	 */
	public void testGetExecutor() {
		assertNull(configuration.getChunkOperations());
		RepeatTemplate executor = new RepeatTemplate();
		configuration.setChunkOperations(executor);
		assertEquals(executor, configuration.getChunkOperations());
		
	}

}
