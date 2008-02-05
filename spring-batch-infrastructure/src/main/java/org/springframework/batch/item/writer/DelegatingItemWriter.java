package org.springframework.batch.item.writer;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class DelegatingItemWriter implements ItemWriter, Skippable, InitializingBean {

	private ItemWriter writer;

	/**
	 * Calls {@link #doProcess(Object)} and then writes the result to the
	 * delegate {@link ItemWriter}.
	 * @throws Exception 
	 * 
	 * @see ItemWriter#process(java.lang.Object)
	 */
	final public void write(Object item) throws Exception {
		Object result = doProcess(item);
		writer.write(result);
	}

	/**
	 * By default returns the argument. This method is an extension point meant
	 * to be overridden by subclasses that implement processing logic.
	 * @throws Exception 
	 */
	protected Object doProcess(Object item) throws Exception {
		return item;
	}

	/**
	 * Setter for {@link ItemWriter}.
	 */
	public void setDelegate(ItemWriter writer) {
		this.writer = writer;
	}

	public void skip() {
		if (writer instanceof Skippable) {
			((Skippable) writer).skip();
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(writer);
	}

}
