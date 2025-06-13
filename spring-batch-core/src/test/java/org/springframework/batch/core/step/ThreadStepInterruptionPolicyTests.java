/*
 * Copyright 2006-2022 the original author or authors.
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
package org.springframework.batch.core.step;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobInterruptedException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Dave Syer
 *
 */
class ThreadStepInterruptionPolicyTests {

	private final ThreadStepInterruptionPolicy policy = new ThreadStepInterruptionPolicy();

	private final StepExecution context = new StepExecution("stepSupport", null);

	@Test
	void testCheckInterruptedNotComplete() {
		assertDoesNotThrow(() -> policy.checkInterrupted(context));
	}

	@Test
	void testCheckInterruptedComplete() {
		context.setTerminateOnly();
		Exception exception = assertThrows(JobInterruptedException.class, () -> policy.checkInterrupted(context));
		assertTrue(exception.getMessage().contains("interrupt"));
	}

}
