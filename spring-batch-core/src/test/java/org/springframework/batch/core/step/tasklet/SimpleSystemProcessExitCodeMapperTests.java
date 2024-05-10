/*
 * Copyright 2008-2024 the original author or authors.
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
package org.springframework.batch.core.step.tasklet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;

/**
 * Tests for {@link SimpleSystemProcessExitCodeMapper}.
 */
class SimpleSystemProcessExitCodeMapperTests {

	private final SimpleSystemProcessExitCodeMapper mapper = new SimpleSystemProcessExitCodeMapper();

	/**
	 * 0 -> ExitStatus.COMPLETED else -> ExitStatus.FAILED
	 */
	@Test
	void testMapping() {
		assertEquals(ExitStatus.COMPLETED, mapper.getExitStatus(0));
		assertEquals(ExitStatus.FAILED, mapper.getExitStatus(1));
		assertEquals(ExitStatus.FAILED, mapper.getExitStatus(-1));
	}

}
