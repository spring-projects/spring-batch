/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.tasklet.ConfigurableSystemProcessExitCodeMapper;

/**
 * Tests for {@link ConfigurableSystemProcessExitCodeMapper}
 */
public class ConfigurableSystemProcessExitCodeMapperTests {

	private ConfigurableSystemProcessExitCodeMapper mapper = new ConfigurableSystemProcessExitCodeMapper();

	/**
	 * Regular usage scenario - mapping adheres to injected values
	 */
	@Test
	public void testMapping() {
		@SuppressWarnings("serial")
		Map<Object, ExitStatus> mappings = new HashMap<Object, ExitStatus>() {
			{
				put(0, ExitStatus.COMPLETED);
				put(1, ExitStatus.FAILED);
				put(2, ExitStatus.EXECUTING);
				put(3, ExitStatus.NOOP);
				put(4, ExitStatus.UNKNOWN);
				put(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY, ExitStatus.UNKNOWN);
			}
		};

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
	public void testSetMappingsMissingElseClause() {
		Map<Object, ExitStatus> missingElse = new HashMap<>();
		try {
			mapper.setMappings(missingElse);
			fail();
		}
		catch (IllegalArgumentException e) {
			// expected
		}

		Map<Object, ExitStatus> containsElse = Collections
				.<Object, ExitStatus>singletonMap(ConfigurableSystemProcessExitCodeMapper.ELSE_KEY, ExitStatus.FAILED);
		// no error expected now
		mapper.setMappings(containsElse);
	}

}
