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
package org.springframework.batch.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Dave Syer
 *
 */
class StepContributionTests {

	private final StepExecution execution = new StepExecution("step", null);

	private final StepContribution contribution = new StepContribution(execution);

	@Test
	void testIncrementFilterCount() {
		assertEquals(0, contribution.getFilterCount());
		contribution.incrementFilterCount(1);
		assertEquals(1, contribution.getFilterCount());
	}

	@Test
	void testEqualsNull() {
		assertNotEquals(null, contribution);
	}

	@Test
	void testEqualsAnother() {
		assertEquals(new StepExecution("foo", null).createStepContribution(), contribution);
		assertEquals(new StepExecution("foo", null).createStepContribution().hashCode(), contribution.hashCode());
	}

}
