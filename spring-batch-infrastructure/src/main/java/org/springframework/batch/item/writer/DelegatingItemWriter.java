package org.springframework.batch.item.writer;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.ExecutionAttributes;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemWriter} providing {@link ItemStream} where
 * the {@link ItemWriter} does.  To make sure 
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

	/**
	 * @see ItemStream#getExecutionAttributes()
	 */
	public ExecutionAttributes getStreamContext() {

		Assert.state(writer != null, "Source must not be null.");

		if (writer instanceof ItemStream) {
			return ((ItemStream) writer).getExecutionAttributes();
		}
		else {
			return new ExecutionAttributes();
		}
	}

	/**
	 * @see ItemStream#restoreFrom(ExecutionAttributes)
	 */
	public void restoreFrom(ExecutionAttributes data) {

		Assert.state(writer != null, "Source must not be null.");

		if (writer instanceof ItemStream) {
			((ItemStream) writer).restoreFrom(data);
		}

	}

	public void skip() {
		if (writer instanceof Skippable) {
			((Skippable) writer).skip();
		}
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(writer);
	}

	// YODO: remove
	public void close() throws Exception {
		writer.close();
	}

}
