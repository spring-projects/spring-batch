/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.util.SerializationUtils;

/**
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
class ExitStatusTests {

	@Test
	void testExitStatusNullDescription() {
		ExitStatus status = new ExitStatus("10", null);
		assertEquals("", status.getExitDescription());
	}

	@Test
	void testExitStatusBooleanInt() {
		ExitStatus status = new ExitStatus("10");
		assertEquals("10", status.getExitCode());
	}

	@Test
	void testExitStatusConstantsContinuable() {
		ExitStatus status = ExitStatus.EXECUTING;
		assertEquals("EXECUTING", status.getExitCode());
	}

	@Test
	void testExitStatusConstantsFinished() {
		ExitStatus status = ExitStatus.COMPLETED;
		assertEquals("COMPLETED", status.getExitCode());
	}

	@Test
	void testEqualsWithSameProperties() {
		assertEquals(ExitStatus.EXECUTING, new ExitStatus("EXECUTING"));
	}

	@Test
	void testEqualsSelf() {
		ExitStatus status = new ExitStatus("test");
		assertEquals(status, status);
	}

	@Test
	void testEquals() {
		assertEquals(new ExitStatus("test"), new ExitStatus("test"));
	}

	@Test
	void testEqualsWithNull() {
		assertNotEquals(null, ExitStatus.EXECUTING);
	}

	@Test
	void testHashcode() {
		assertEquals(ExitStatus.EXECUTING.toString().hashCode(), ExitStatus.EXECUTING.hashCode());
	}

	@Test
	void testAndExitStatusStillExecutable() {
		assertEquals(ExitStatus.EXECUTING.getExitCode(), ExitStatus.EXECUTING.and(ExitStatus.EXECUTING).getExitCode());
	}

	@Test
	void testAndExitStatusWhenFinishedAddedToContinuable() {
		assertEquals(ExitStatus.COMPLETED.getExitCode(), ExitStatus.EXECUTING.and(ExitStatus.COMPLETED).getExitCode());
	}

	@Test
	void testAndExitStatusWhenContinuableAddedToFinished() {
		assertEquals(ExitStatus.COMPLETED.getExitCode(), ExitStatus.COMPLETED.and(ExitStatus.EXECUTING).getExitCode());
	}

	@Test
	void testAndExitStatusWhenCustomContinuableAddedToContinuable() {
		assertEquals("CUSTOM", ExitStatus.EXECUTING.and(ExitStatus.EXECUTING.replaceExitCode("CUSTOM")).getExitCode());
	}

	@Test
	void testAndExitStatusWhenCustomCompletedAddedToCompleted() {
		assertEquals("COMPLETED_CUSTOM",
				ExitStatus.COMPLETED.and(ExitStatus.EXECUTING.replaceExitCode("COMPLETED_CUSTOM")).getExitCode());
	}

	@Test
	void testAndExitStatusFailedPlusFinished() {
		assertEquals("FAILED", ExitStatus.COMPLETED.and(ExitStatus.FAILED).getExitCode());
		assertEquals("FAILED", ExitStatus.FAILED.and(ExitStatus.COMPLETED).getExitCode());
	}

	@Test
	void testAndExitStatusWhenCustomContinuableAddedToFinished() {
		assertEquals("CUSTOM", ExitStatus.COMPLETED.and(ExitStatus.EXECUTING.replaceExitCode("CUSTOM")).getExitCode());
	}

	@Test
	void testAddExitCode() {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO");
		assertNotSame(ExitStatus.EXECUTING, status);
		assertEquals("FOO", status.getExitCode());
	}

	@Test
	void testAddExitCodeToExistingStatus() {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO").replaceExitCode("BAR");
		assertNotSame(ExitStatus.EXECUTING, status);
		assertEquals("BAR", status.getExitCode());
	}

	@Test
	void testAddExitCodeToSameStatus() {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode(ExitStatus.EXECUTING.getExitCode());
		assertNotSame(ExitStatus.EXECUTING, status);
		assertEquals(ExitStatus.EXECUTING.getExitCode(), status.getExitCode());
	}

	@Test
	void testAddExitDescription() {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo");
		assertNotSame(ExitStatus.EXECUTING, status);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	void testAddExitDescriptionWIthStacktrace() {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription(new RuntimeException("Foo"));
		assertNotSame(ExitStatus.EXECUTING, status);
		String description = status.getExitDescription();
		assertTrue(description.contains("Foo"), "Wrong description: " + description);
		assertTrue(description.contains("RuntimeException"), "Wrong description: " + description);
	}

	@Test
	void testAddExitDescriptionToSameStatus() {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo").addExitDescription("Foo");
		assertNotSame(ExitStatus.EXECUTING, status);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	void testAddEmptyExitDescription() {
		ExitStatus status = ExitStatus.EXECUTING.addExitDescription("Foo").addExitDescription((String) null);
		assertEquals("Foo", status.getExitDescription());
	}

	@Test
	void testAddExitCodeWithDescription() {
		ExitStatus status = new ExitStatus("BAR", "Bar").replaceExitCode("FOO");
		assertEquals("FOO", status.getExitCode());
		assertEquals("Bar", status.getExitDescription());
	}

	@Test
	void testUnknownIsRunning() {
		assertTrue(ExitStatus.UNKNOWN.isRunning());
	}

	@Test
	void testSerializable() {
		ExitStatus status = ExitStatus.EXECUTING.replaceExitCode("FOO");
		ExitStatus clone = SerializationUtils.clone(status);
		assertEquals(status.getExitCode(), clone.getExitCode());
	}

}
