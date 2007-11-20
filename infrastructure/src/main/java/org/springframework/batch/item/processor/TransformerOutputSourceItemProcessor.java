package org.springframework.batch.item.processor;

import org.springframework.batch.io.OutputSource;
import org.springframework.util.Assert;

/**
 * Transforms the item using injected {@link ItemTransformer}
 * before it is written to output by {@link OutputSource}.
 * 
 * @author Robert Kasanicky
 */
public class TransformerOutputSourceItemProcessor extends OutputSourceItemProcessor {

	private ItemTransformer itemTransformer;

	/**
	 * Transform the item using the {@link #itemTransformer}.
	 */
	protected Object doProcess(Object item) {
		return itemTransformer.transform(item);
	}

	/**
	 * @param itemTransformer will transform the item before
	 * it is passed to {@link OutputSource}.
	 */
	public void setItemTransformer(ItemTransformer itemTransformer) {
		this.itemTransformer = itemTransformer;
	}

	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
		Assert.notNull(itemTransformer);
	}
}
