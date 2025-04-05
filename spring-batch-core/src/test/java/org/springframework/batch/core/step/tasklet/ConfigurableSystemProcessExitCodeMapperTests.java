/*
 * Copyright 2008-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;

/**
 * Tests for {@link ConfigurableSystemProcessExitCodeMapper}
 */
class ConfigurableSystemProcessExitCodeMapperTests {

	private final ConfigurableSystemProcessExitCodeMapper mapper = new ConfigurableSystemProcessExitCodeMapper();

	/**
	 * Regular usage scenario - mapping adheres to injected values
	 */
	@Test
	void testMapping() {
		Map<Object, ExitStatus> mappings = Map.of( //
				0, ExitStatus.COMPLETED, //
				1, ExitStatus.FAILED, //
				2, ExitStatus.EXECUTING, //
				3, ExitStatus.NOOP, //
				4, ExitStatus.UNKNOWN, //
				ConfigurableSystemProcessExitCodeMapper.ELSE_KEY, ExitStatus.UNKNOWN);

		mapper.setMappings(mappings);

		// check explicitly defined values
		for (Map.Entry<Object, ExitStatus> entry : mappings.entrySet()) {
			if (entry.getKey().equals(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY))
				continue;

			int exitCode = (Integer) entry.getKey();
			assertSame(entry.getValue(), mapper.getExitStatus(exitCode));
		}

		// check the else clause
		assertSame(mappings.get(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY), mapper.getExitStatus(5));
	}

	/**
	 * Else clause is required in the injected map - setter checks its presence.
	 */
	@Test
	void testSetMappingsMissingElseClause() {
		Map<Object, ExitStatus> missingElse = new HashMap<>();
		assertThrows(IllegalArgumentException.class, () -> mapper.setMappings(missingElse));

		Map<Object, ExitStatus> containsElse = Map.of(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY,
				ExitStatus.FAILED);
		// no error expected now
		mapper.setMappings(containsElse);
	}

}
