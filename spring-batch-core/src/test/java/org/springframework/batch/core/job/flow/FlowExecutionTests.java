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
package org.springframework.batch.core.job.flow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * @author Dave Syer
 *
 */
class FlowExecutionTests {

	@Test
	void testBasicProperties() {
		FlowExecution execution = new FlowExecution("foo", new FlowExecutionStatus("BAR"));
		assertEquals("foo", execution.getName());
		assertEquals("BAR", execution.getStatus().getName());
	}

	@Test
	void testAlphaOrdering() {
		FlowExecution first = new FlowExecution("foo", new FlowExecutionStatus("BAR"));
		FlowExecution second = new FlowExecution("foo", new FlowExecutionStatus("SPAM"));
		assertTrue(first.compareTo(second) < 0, "Should be negative");
		assertTrue(second.compareTo(first) > 0, "Should be positive");
	}

	@Test
	void testEnumOrdering() {
		FlowExecution first = new FlowExecution("foo", FlowExecutionStatus.COMPLETED);
		FlowExecution second = new FlowExecution("foo", FlowExecutionStatus.FAILED);
		assertTrue(first.compareTo(second) < 0, "Should be negative");
		assertTrue(second.compareTo(first) > 0, "Should be positive");
	}

	@Test
	void testEnumStartsWithOrdering() {
		FlowExecution first = new FlowExecution("foo", new FlowExecutionStatus("COMPLETED.BAR"));
		FlowExecution second = new FlowExecution("foo", new FlowExecutionStatus("FAILED.FOO"));
		assertTrue(first.compareTo(second) < 0, "Should be negative");
		assertTrue(second.compareTo(first) > 0, "Should be positive");
	}

	@Test
	void testEnumStartsWithAlphaOrdering() {
		FlowExecution first = new FlowExecution("foo", new FlowExecutionStatus("COMPLETED.BAR"));
		FlowExecution second = new FlowExecution("foo", new FlowExecutionStatus("COMPLETED.FOO"));
		assertTrue(first.compareTo(second) < 0, "Should be negative");
		assertTrue(second.compareTo(first) > 0, "Should be positive");
	}

	@Test
	void testEnumAndAlpha() {
		FlowExecution first = new FlowExecution("foo", new FlowExecutionStatus("ZZZZZ"));
		FlowExecution second = new FlowExecution("foo", new FlowExecutionStatus("FAILED.FOO"));
		assertTrue(first.compareTo(second) < 0, "Should be negative");
		assertTrue(second.compareTo(first) > 0, "Should be positive");
	}

}
