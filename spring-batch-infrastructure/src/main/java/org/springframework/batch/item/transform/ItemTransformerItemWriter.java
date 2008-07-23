package org.springframework.batch.item.transform;

import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.DelegatingItemWriter;
import org.springframework.util.Assert;

/**
 * Transforms the item using injected {@link ItemTransformer}
 * before it is written to output by {@link ItemWriter}.
 * 
 * @author Robert Kasanicky
 */
public class ItemTransformerItemWriter<I,O> extends DelegatingItemWriter<I,O> {

	private ItemTransformer<I,O> itemTransformer;

	/**
	 * Transform the item using the {@link #setItemTransformer(ItemTransformer)}.
	 */
	protected O doProcess(I item) throws Exception {
		return itemTransformer.transform(item);
	}

	/**
	 * @param itemTransformer will transform the item before
	 * it is passed to {@link ItemWriter}.
	 */
	public void setItemTransformer(ItemTransformer<I,O> itemTransformer) {
		this.itemTransformer = itemTransformer;
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(itemTransformer);
	}
}
