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

package org.springframework.batch.retry.backoff;

import junit.framework.TestCase;

/**
 * @author Rob Harrop
 * @author Dave Syer
 * @since 2.1
 */
public class FixedBackOffPolicyTests extends TestCase {

	public void testSetBackoffPeriodNegative() throws Exception {
		FixedBackOffPolicy strategy = new FixedBackOffPolicy();
		strategy.setBackOffPeriod(-1000L);
		long before = System.currentTimeMillis();
		strategy.backOff(null);
		long after = System.currentTimeMillis();
		// We should see a zero backoff if we try to set it negative
		assertEqualsApprox(0, after - before, 25);
	}

	public void testSingleBackOff() throws Exception {
		int backOffPeriod = 50;
		FixedBackOffPolicy strategy = new FixedBackOffPolicy();
		strategy.setBackOffPeriod(backOffPeriod);
		long before = System.currentTimeMillis();
		strategy.backOff(null);
		long after = System.currentTimeMillis();
		assertEqualsApprox(backOffPeriod, after - before, 25);
	}

	public void testManyBackOffCalls() throws Exception {
		int backOffPeriod = 50;
		FixedBackOffPolicy strategy = new FixedBackOffPolicy();
		strategy.setBackOffPeriod(backOffPeriod);
		for (int x = 0; x < 10; x++) {
			long before = System.currentTimeMillis();
			strategy.backOff(null);
			long after = System.currentTimeMillis();
			assertEqualsApprox(backOffPeriod, after - before, 25);
		}
	}

	private void assertEqualsApprox(long desired, long actual, long variance) {
		long lower = desired - variance;
		long upper = desired + 2 * variance;
		assertTrue("Expected value to be between '" + lower + "' and '" + upper + "' but was '" + actual + "'",
				lower <= actual && actual <= upper);
	}
}
