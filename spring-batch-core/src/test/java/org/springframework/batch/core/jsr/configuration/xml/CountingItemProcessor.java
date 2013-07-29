package org.springframework.batch.core.jsr.configuration.xml;

import javax.batch.api.chunk.ItemProcessor;


public class CountingItemProcessor implements ItemProcessor {
	protected int count = 0;

	@Override
	public Object processItem(Object item) throws Exception {
		count++;
		return item;
	}
}
