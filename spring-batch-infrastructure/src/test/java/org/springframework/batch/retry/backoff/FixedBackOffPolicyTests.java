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

	private DummySleeper sleeper = new DummySleeper();

	public void testSetBackoffPeriodNegative() throws Exception {
		FixedBackOffPolicy strategy = new FixedBackOffPolicy();
		strategy.setBackOffPeriod(-1000L);
		strategy.setSleeper(sleeper);
		strategy.backOff(null);
		// We should see a zero backoff if we try to set it negative
		assertEquals(1, sleeper.getBackOffs().length);
		assertEquals(1, sleeper.getLastBackOff());
	}

	public void testSingleBackOff() throws Exception {
		int backOffPeriod = 50;
		FixedBackOffPolicy strategy = new FixedBackOffPolicy();
		strategy.setBackOffPeriod(backOffPeriod);
		strategy.setSleeper(sleeper);
		strategy.backOff(null);
		assertEquals(1, sleeper.getBackOffs().length);
		assertEquals(backOffPeriod, sleeper.getLastBackOff());
	}

	public void testManyBackOffCalls() throws Exception {
		int backOffPeriod = 50;
		FixedBackOffPolicy strategy = new FixedBackOffPolicy();
		strategy.setBackOffPeriod(backOffPeriod);
		strategy.setSleeper(sleeper);
		for (int x = 0; x < 10; x++) {
			strategy.backOff(null);
			assertEquals(backOffPeriod, sleeper.getLastBackOff());
		}
		assertEquals(10, sleeper.getBackOffs().length);
	}

}
