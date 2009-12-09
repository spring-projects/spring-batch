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

		public RepeatContext getContext() {
			return null;
		}

		public Throwable getError() {
			return error;
		}

		public RepeatStatus getResult() {
			return result;
		}
	}

}
