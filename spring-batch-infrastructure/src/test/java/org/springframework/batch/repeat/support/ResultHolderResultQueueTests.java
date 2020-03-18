/*
 * Copyright 2009-2012 the original author or authors.
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
package org.springframework.batch.repeat.support;


import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;

public class ResultHolderResultQueueTests {

	private ResultHolderResultQueue queue = new ResultHolderResultQueue(10);

	@Test
	public void testPutTake() throws Exception {
		queue.expect();
		assertTrue(queue.isExpecting());
		assertTrue(queue.isEmpty());
		queue.put(new TestResultHolder(RepeatStatus.CONTINUABLE));
		assertFalse(queue.isEmpty());
		assertTrue(queue.take().getResult().isContinuable());
		assertFalse(queue.isExpecting());
	}

	@Test
	public void testOrdering() throws Exception {
		queue.expect();
		queue.expect();
		queue.put(new TestResultHolder(RepeatStatus.FINISHED));
		queue.put(new TestResultHolder(RepeatStatus.CONTINUABLE));
		assertFalse(queue.isEmpty());
		assertTrue(queue.take().getResult().isContinuable());
		assertFalse(queue.take().getResult().isContinuable());
	}

	private static class TestResultHolder implements ResultHolder {

		private RepeatStatus result;

		private Throwable error;

		public TestResultHolder(RepeatStatus result) {
			super();
			this.result = result;
		}

        @Override
		public RepeatContext getContext() {
			return null;
		}

        @Override
		public Throwable getError() {
			return error;
		}

        @Override
		public RepeatStatus getResult() {
			return result;
		}
	}

}
