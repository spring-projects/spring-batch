/*
 * Copyright 2008-2022 the original author or authors.
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
package org.springframework.batch.core.partition.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.partition.StepExecutionSplitter;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

class TaskExecutorPartitionHandlerTests {

	private TaskExecutorPartitionHandler handler = new TaskExecutorPartitionHandler();

	private int count = 0;

	private final Collection<String> stepExecutions = new TreeSet<>();

	private final StepExecution stepExecution = new StepExecution("step", new JobExecution(1L));

	private final StepExecutionSplitter stepExecutionSplitter = new StepExecutionSplitter() {

		@Override
		public String getStepName() {
			return stepExecution.getStepName();
		}

		@Override
		public Set<StepExecution> split(StepExecution stepExecution, int gridSize) throws JobExecutionException {
			HashSet<StepExecution> result = new HashSet<>();
			for (int i = gridSize; i-- > 0;) {
				result.add(stepExecution.getJobExecution().createStepExecution("foo" + i));
			}
			return result;
		}
	};

	@BeforeEach
	void setUp() throws Exception {
		handler.setStep(new StepSupport() {
			@Override
			public void execute(StepExecution stepExecution) throws JobInterruptedException {
				count++;
				stepExecutions.add(stepExecution.getStepName());
			}
		});
		handler.afterPropertiesSet();
	}

	@Test
	void testConfiguration() {
		handler = new TaskExecutorPartitionHandler();
		Exception exception = assertThrows(IllegalStateException.class, handler::afterPropertiesSet);
		String message = exception.getMessage();
		assertEquals("A Step must be provided.", message, "Wrong message: " + message);
	}

	@Test
	void testNullStep() {
		handler = new TaskExecutorPartitionHandler();
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> handler.handle(stepExecutionSplitter, stepExecution));
		String message = exception.getMessage();
		assertTrue(message.contains("Step"), "Wrong message: " + message);
	}

	@Test
	void testSetGridSize() throws Exception {
		handler.setGridSize(2);
		handler.handle(stepExecutionSplitter, stepExecution);
		assertEquals(2, count);
		assertEquals("[foo0, foo1]", stepExecutions.toString());
	}

	@Test
	void testSetTaskExecutor() throws Exception {
		handler.setTaskExecutor(new SimpleAsyncTaskExecutor());
		handler.handle(stepExecutionSplitter, stepExecution);
		assertEquals(1, count);
	}

	@Test
	void testTaskExecutorFailure() throws Exception {
		handler.setGridSize(2);
		handler.setTaskExecutor(new TaskExecutor() {
			@Override
			public void execute(Runnable task) {
				if (count > 0) {
					throw new TaskRejectedException("foo");
				}
				task.run();
			}
		});
		Collection<StepExecution> executions = handler.handle(stepExecutionSplitter, stepExecution);
		new DefaultStepExecutionAggregator().aggregate(stepExecution, executions);
		assertEquals(1, count);
		assertEquals(ExitStatus.FAILED.getExitCode(), stepExecution.getExitStatus().getExitCode());
	}

}
