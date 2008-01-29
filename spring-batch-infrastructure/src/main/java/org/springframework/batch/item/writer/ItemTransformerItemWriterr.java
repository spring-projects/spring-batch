package org.springframework.batch.item.writer;

import org.springframework.batch.item.ItemWriter;
import org.springframework.util.Assert;

/**
 * Transforms the item using injected {@link ItemTransformer}
 * before it is written to output by {@link ItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class ItemTransformerItemWriterr extends DelegatingItemWriter {

	private ItemTransformer itemTransformer;

	/**
	 * Transform the item using the {@link #itemTransformer}.
	 */
	protected Object doProcess(Object item) throws Exception {
		return itemTransformer.transform(item);
	}

	/**
	 * @param itemTransformer will transform the item before
	 * it is passed to {@link ItemWriter}.
	 */
	public void setItemTransformer(ItemTransformer itemTransformer) {
		this.itemTransformer = itemTransformer;
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(itemTransformer);
	}
}
