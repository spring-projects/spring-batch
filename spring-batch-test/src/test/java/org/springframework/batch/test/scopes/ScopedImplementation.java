package org.springframework.batch.test.scopes;

import java.util.concurrent.atomic.AtomicInteger;

public class ScopedImplementation implements ScopedInterface {

	private final static AtomicInteger INSTANCE_COUNTER = new AtomicInteger();

	private final int instance;

	public ScopedImplementation() {
		this.instance = INSTANCE_COUNTER.incrementAndGet();
	}

	@Override
	public int getInstanceNumber() {
		return instance;
	}

}
