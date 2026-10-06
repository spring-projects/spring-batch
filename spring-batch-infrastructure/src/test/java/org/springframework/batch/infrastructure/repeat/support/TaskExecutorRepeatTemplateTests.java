/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.batch.infrastructure.repeat.support;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sharang Gupta
 */
@Timeout(value = 10, unit = TimeUnit.SECONDS)
class TaskExecutorRepeatTemplateTests {

	private final TaskExecutorRepeatTemplate template = new TaskExecutorRepeatTemplate();

	/**
	 * Regression test for https://github.com/spring-projects/spring-batch/issues/3948:
	 * when the task executor fails to run a task (for example a step-scoped executor
	 * whose target bean cannot be created), the iteration used to wait forever for the
	 * result of a task that never ran.
	 */
	@Test
	void testFailureToExecuteTaskIsRethrownInsteadOfHanging() {
		RuntimeException executorFailure = new IllegalStateException("Error creating bean 'taskExecutor'");
		template.setTaskExecutor(task -> {
			throw executorFailure;
		});

		Exception exception = assertThrows(IllegalStateException.class,
				() -> template.iterate(context -> RepeatStatus.CONTINUABLE));

		assertSame(executorFailure, exception);
	}

	@Test
	void testRejectedTaskIsRethrownAfterAcceptedTasksComplete() {
		int acceptedTasks = 2;
		AtomicInteger executedTasks = new AtomicInteger();
		template.setTaskExecutor(rejectingAfter(acceptedTasks));

		assertThrows(TaskRejectedException.class, () -> template.iterate(context -> {
			executedTasks.incrementAndGet();
			return RepeatStatus.CONTINUABLE;
		}));

		assertEquals(acceptedTasks, executedTasks.get());
	}

	private static TaskExecutor rejectingAfter(int acceptedTasks) {
		TaskExecutor delegate = new SimpleAsyncTaskExecutor();
		AtomicInteger submittedTasks = new AtomicInteger();
		return task -> {
			if (submittedTasks.incrementAndGet() > acceptedTasks) {
				throw new TaskRejectedException("Thread pool exhausted");
			}
			delegate.execute(task);
		};
	}

}
