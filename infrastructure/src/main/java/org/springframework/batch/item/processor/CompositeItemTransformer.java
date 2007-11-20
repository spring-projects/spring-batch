package org.springframework.batch.item.processor;

import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Composite {@link ItemTransformer} that passes the item through a sequence
 * of injected <code>ItemTransformer</code>s (return value of previous transformation
 * is the entry value of the next).
 * 
 * @author Robert Kasanicky
 */
public class CompositeItemTransformer implements ItemTransformer, InitializingBean {

	private List itemTransformers;
	
	public Object transform(Object item) {
		Object result = item;
		for (Iterator iterator = itemTransformers.listIterator(); iterator.hasNext();) {
			result = ((ItemTransformer)iterator.next()).transform(result);	
		}
		return result;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notEmpty(itemTransformers);
		for (Iterator iterator = itemTransformers.iterator(); iterator.hasNext();) {
			Assert.isInstanceOf(ItemTransformer.class, iterator.next());
		}
	}

	/**
	 * @param itemTransformers will be chained to produce a composite transformation.
	 */
	public void setItemTransformers(List itemTransformers) {
		this.itemTransformers = itemTransformers;
	}

}
