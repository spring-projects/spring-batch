package org.springframework.batch.jsr.item;

import javax.batch.api.chunk.ItemProcessor;

import org.springframework.util.Assert;

@SuppressWarnings("rawtypes")
public class ItemProcessorAdapter implements org.springframework.batch.item.ItemProcessor {

	private ItemProcessor delegate;

	public ItemProcessorAdapter(ItemProcessor processor) {
		Assert.notNull(processor, "An ItemProcessor implementation is required");
		this.delegate = processor;
	}

	@Override
	public Object process(Object item) throws Exception {
		return delegate.processItem(item);
	}
}
