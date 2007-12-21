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
public class ExponentialBackOffPolicyTests extends TestCase {

	public void testSetMaxInterval() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		strategy.setMaxInterval(1000);
		assertTrue(strategy.toString().indexOf("maxInterval=1000") >= 0);
		strategy.setMaxInterval(0);
		// The minimum value for the max interval is 1
		assertTrue(strategy.toString().indexOf("maxInterval=1") >= 0);
	}

	public void testSetInitialInterval() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		strategy.setInitialInterval(10000);
		assertTrue(strategy.toString().indexOf("initialInterval=10000,") >= 0);
		strategy.setInitialInterval(0);
		assertTrue(strategy.toString().indexOf("initialInterval=1,") >= 0);
	}

	public void testSetMultiplier() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		strategy.setMultiplier(3.);
		assertTrue(strategy.toString().indexOf("multiplier=3.") >= 0);
		strategy.setMultiplier(.5);
		assertTrue(strategy.toString().indexOf("multiplier=1.") >= 0);
	}

	public void testSingleBackOff() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		BackOffContext context = strategy.start(null);
		long before = System.currentTimeMillis();
		strategy.backOff(context);
		long after = System.currentTimeMillis();
		assertEqualsApprox(ExponentialBackOffPolicy.DEFAULT_INITIAL_INTERVAL, after - before, 30);
	}

	public void testMaximumBackOff() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		strategy.setMaxInterval(50);
		BackOffContext context = strategy.start(null);
		long before = System.currentTimeMillis();
		strategy.backOff(context);
		long after = System.currentTimeMillis();
		assertEqualsApprox(50L, after - before, 15);
	}

	public void testMultiBackOff() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		long seed = 40; // not too small or Windoze won't resolve the difference
		double multiplier = 1.2; // not too large or the test takes ages!
		strategy.setInitialInterval(seed);
		strategy.setMultiplier(multiplier);
		BackOffContext context = strategy.start(null);
		for (int x = 0; x < 5; x++) {
			long before = System.currentTimeMillis();
			strategy.backOff(context);
			long after = System.currentTimeMillis();
			assertFalse(after == before);
			assertEqualsApprox(seed, after - before, 20);
			seed *= multiplier;
		}
	}

	private void assertEqualsApprox(long desired, long actual, long variance) {
		long lower = desired - variance;
		long upper = desired + 5 * variance / 2;
		assertTrue("Expected value to be between '" + lower + "' and '" + upper + "' but was '" + actual + "'",
				lower <= actual);
	}
}
