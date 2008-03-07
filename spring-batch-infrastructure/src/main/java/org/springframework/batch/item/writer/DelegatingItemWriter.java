package org.springframework.batch.item.writer;

import org.springframework.batch.item.ClearFailedException;
import org.springframework.batch.item.FlushFailedException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemWriter}.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class DelegatingItemWriter implements ItemWriter, InitializingBean {

	private ItemWriter writer;
	
	/**
	 * Default constructor.
	 */
	public DelegatingItemWriter() {
		super();
	}
	
	/**
	 * @param itemWriter
	 */
	public DelegatingItemWriter(ItemWriter itemWriter) {
		this();
		this.writer = itemWriter;
	}

	/**
	 * Calls {@link #doProcess(Object)} and then writes the result to the
	 * delegate {@link ItemWriter}.
	 * @throws Exception 
	 * 
	 * @see ItemWriter#process(java.lang.Object)
	 */
	public void write(Object item) throws Exception {
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

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(writer);
	}

	/**
	 * Delegates to {@link ItemWriter#clear()}
	 */
	public void clear() throws ClearFailedException {
		writer.clear();
	}

	/**
	 * Delegates to {@link ItemWriter#flush()}
	 */
	public void flush() throws FlushFailedException {
		writer.flush();
	}

}
