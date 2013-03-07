package org.springframework.batch.core.test.timeout;

import org.springframework.batch.item.ItemProcessor;

public class SleepingItemProcessor<I> implements ItemProcessor<I, I> {
	
	private long millisToSleep;

	@Override
	public I process(I item) throws Exception {
		Thread.sleep(millisToSleep);
		return item;
	}
	
	public void setMillisToSleep(long millisToSleep) {
		this.millisToSleep = millisToSleep;
	}

}
