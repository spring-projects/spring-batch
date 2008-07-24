package org.springframework.batch.item.transform;

import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Composite {@link ItemTransformer} that passes the item through a sequence of
 * injected <code>ItemTransformer</code>s (return value of previous
 * transformation is the entry value of the next).
 * 
 * Note the user is responsible for injecting a chain of {@link ItemTransformer}
 * s that conforms to declared input and output types.
 * 
 * @author Robert Kasanicky
 */
@SuppressWarnings("unchecked")
public class CompositeItemTransformer<I, O> implements ItemTransformer<I, O>, InitializingBean {

	private List<ItemTransformer> itemTransformers;

	public O transform(I item) throws Exception {
		Object result = item;
	
		for(ItemTransformer transformer: itemTransformers){
			result = transformer.transform(result);
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
	public void setItemTransformers(List<ItemTransformer> itemTransformers) {
		this.itemTransformers = itemTransformers;
	}

}
