package org.springframework.batch.item.support;

import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Composite {@link ItemProcessor} that passes the item through a sequence of
 * injected <code>ItemTransformer</code>s (return value of previous
 * transformation is the entry value of the next).
 * 
 * Note the user is responsible for injecting a chain of {@link ItemProcessor}
 * s that conforms to declared input and output types.
 * 
 * @author Robert Kasanicky
 */
@SuppressWarnings("unchecked")
public class CompositeItemProcessor<I, O> implements ItemProcessor<I, O>, InitializingBean {

	private List<ItemProcessor> itemTransformers;

	public O process(I item) throws Exception {
		Object result = item;
	
		for(ItemProcessor transformer: itemTransformers){
			result = transformer.process(result);
		}
		return (O) result;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notEmpty(itemTransformers);
	}

	/**
	 * @param itemTransformers will be chained to produce a composite
	 * transformation.
	 */
	public void setItemTransformers(List<ItemProcessor> itemTransformers) {
		this.itemTransformers = itemTransformers;
	}

}
