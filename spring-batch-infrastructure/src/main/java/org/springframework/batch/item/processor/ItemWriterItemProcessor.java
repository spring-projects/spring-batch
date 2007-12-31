package org.springframework.batch.item.processor;

import java.util.Properties;

import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link ItemWriter} providing {@link Restartable} and
 * {@link StatisticsProvider} where the {@link ItemWriter} does.
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 */
public class ItemWriterItemProcessor implements ItemProcessor, Restartable, Skippable,
		StatisticsProvider, InitializingBean {

	private ItemWriter writer;

	/**
	 * Calls {@link #doProcess(Object)} and then writes the result to the {@link ItemWriter}.
	 * 
	 * @see org.springframework.batch.item.ItemProcessor#process(java.lang.Object)
	 */
	final public void process(Object item) throws Exception {
		Object result = doProcess(item);
		writer.write(result);
	}

	/**
	 * By default returns the argument. This method is an extension point
	 * meant to be overridden by subclasses that implement processing logic. 
	 */
	protected Object doProcess(Object item) throws Exception {
		return item;
	}

	/**
	 * Setter for {@link ItemWriter}.
	 */
	public void setItemWriter(ItemWriter writer) {
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
		else{
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

	/**
	 * @return delegates to the parent template of it is a
	 * {@link StatisticsProvider}, otherwise returns an empty
	 * {@link Properties} instance.
	 * @see StatisticsProvider#getStatistics()
	 */
	public Properties getStatistics() {
		if (!(writer instanceof StatisticsProvider)) {
			return new Properties();
		}
		return ((StatisticsProvider) writer).getStatistics();
	}

	public void skip() {
		if (writer instanceof Skippable) {
			((Skippable)writer).skip();
		}
	}

	
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(writer);
	}

}
