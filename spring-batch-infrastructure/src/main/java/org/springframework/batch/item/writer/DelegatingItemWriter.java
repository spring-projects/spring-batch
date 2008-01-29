package org.springframework.batch.item.writer;

import java.util.Properties;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemWriter} providing {@link Restartable} where
 * the {@link ItemWriter} does.  To make sure 
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class DelegatingItemWriter implements ItemWriter, Restartable, Skippable, InitializingBean {

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
	 * @see Restartable#getRestartData()
	 */
	public RestartData getRestartData() {

		Assert.state(writer != null, "Source must not be null.");

		if (writer instanceof Restartable) {
			return ((Restartable) writer).getRestartData();
		}
		else {
			return new GenericRestartData(new Properties());
		}
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 */
	public void restoreFrom(RestartData data) {

		Assert.state(writer != null, "Source must not be null.");

		if (writer instanceof Restartable) {
			((Restartable) writer).restoreFrom(data);
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

	public void close() throws Exception {
		writer.close();
	}

}
