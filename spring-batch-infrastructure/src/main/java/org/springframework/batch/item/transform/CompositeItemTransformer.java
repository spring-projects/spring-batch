package org.springframework.batch.item.transform;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Composite {@link ItemTransformer} that passes the item through a sequence of
 * injected <code>ItemTransformer</code>s (return value of previous
 * transformation is the entry value of the next).
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemTransformer implements ItemTransformer, InitializingBean {

	private List<ItemTransformer> itemTransformers;

	public Object transform(Object item) throws Exception {
		Object result = item;
		for (Iterator<ItemTransformer> iterator = itemTransformers.listIterator(); iterator.hasNext();) {
			result = iterator.next().transform(result);
		}
		return result;
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
