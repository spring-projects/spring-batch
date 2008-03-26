package org.springframework.batch.repeat.context;

/**
 * @author Dave Syer
 * 
 */
class JdkConcurrentAtomicCounter implements AtomicCounter {

	private java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger();

	public void addAndGet(int delta) {
		counter.addAndGet(delta);
	}

	public int intValue() {
		return counter.intValue();
	}

}