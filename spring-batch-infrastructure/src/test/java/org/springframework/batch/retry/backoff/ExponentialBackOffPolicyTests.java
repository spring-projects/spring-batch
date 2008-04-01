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
 */
public class ExponentialBackOffPolicyTests extends TestCase {

	private DummySleeper sleeper = new DummySleeper();

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
		strategy.setSleeper(sleeper);
		BackOffContext context = strategy.start(null);
		strategy.backOff(context);
		assertEquals(ExponentialBackOffPolicy.DEFAULT_INITIAL_INTERVAL, sleeper.getLastBackOff());
	}

	public void testMaximumBackOff() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		strategy.setMaxInterval(50);
		strategy.setSleeper(sleeper);
		BackOffContext context = strategy.start(null);
		strategy.backOff(context);
		assertEquals(50, sleeper.getLastBackOff());
	}

	public void testMultiBackOff() throws Exception {
		ExponentialBackOffPolicy strategy = new ExponentialBackOffPolicy();
		long seed = 40;
		double multiplier = 1.2;
		strategy.setInitialInterval(seed);
		strategy.setMultiplier(multiplier);
		strategy.setSleeper(sleeper);
		BackOffContext context = strategy.start(null);
		for (int x = 0; x < 5; x++) {
			strategy.backOff(context);
			assertEquals(seed, sleeper.getLastBackOff());
			seed *= multiplier;
		}
	}

}
