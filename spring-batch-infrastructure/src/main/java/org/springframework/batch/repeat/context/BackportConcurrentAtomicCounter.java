package org.springframework.batch.repeat.context;

/**
 * @author Dave Syer
 * 
 */
class BackportConcurrentAtomicCounter implements AtomicCounter {

	private edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger counter = new edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicInteger();

	public void addAndGet(int delta) {
		counter.addAndGet(delta);
	}

	public int intValue() {
		return counter.intValue();
	}

}