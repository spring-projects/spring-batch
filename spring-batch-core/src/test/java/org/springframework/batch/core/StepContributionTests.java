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
package org.springframework.batch.core;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * @author Dave Syer
 * 
 */
public class StepContributionTests extends TestCase {

	private StepExecution execution = new StepExecution("step", null);

	private StepContribution contribution = new StepContribution(execution);

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.StepContribution#incrementFilterCount(int)}
	 * .
	 */
	public void testIncrementFilterCount() {
		assertEquals(0, contribution.getFilterCount());
		contribution.incrementFilterCount(1);
		assertEquals(1, contribution.getFilterCount());
	}

	@Test
	public void testEqualsNull() throws Exception {
		assertFalse(contribution.equals(null));
	}

	@Test
	public void testEqualsAnother() throws Exception {
		assertEquals(new StepExecution("foo", null).createStepContribution(), contribution);
		assertEquals(new StepExecution("foo", null).createStepContribution().hashCode(), contribution.hashCode());
	}
}
