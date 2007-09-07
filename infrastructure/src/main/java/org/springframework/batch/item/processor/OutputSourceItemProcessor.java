package org.springframework.batch.item.processor;

import java.util.Properties;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.restart.GenericRestartData;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;
import org.springframework.util.Assert;

/**
 * Simple wrapper around {@link OutputSource} providing {@link Restartable} and
 * {@link StatisticsProvider} where the {@link OutputSource} does.
 * 
 * @author Dave Syer
 */
public class OutputSourceItemProcessor implements ItemProcessor, Restartable, Skippable,
		StatisticsProvider {

	private OutputSource source;

	/* (non-Javadoc)
	 * @see org.springframework.batch.item.ItemProcessor#process(java.lang.Object)
	 */
	public void process(Object data) throws Exception {
		source.write(data);
	}

	/**
	 * Setter for output source.
	 * 
	 * @param source
	 */
	public void setOutputSource(OutputSource source) {
		this.source = source;
	}

	/**
	 * @see Restartable#getRestartData()
	 */
	public RestartData getRestartData() {
		
		Assert.state(source != null, "Source must not be null.");
		
		if (source instanceof Restartable) {
			return ((Restartable) source).getRestartData();
		}
		else{
			return new GenericRestartData(new Properties());
		}
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 */
	public void restoreFrom(RestartData data) {
		
		Assert.state(source != null, "Source must not be null.");
		
		if (source instanceof Restartable) {
			((Restartable) source).restoreFrom(data);
		}
		
	}

	/**
	 * @return delegates to the parent template of it is a
	 * {@link StatisticsProvider}, otherwise returns an empty
	 * {@link Properties} instance.
	 * @see StatisticsProvider#getStatistics()
	 */
	public Properties getStatistics() {
		if (!(source instanceof StatisticsProvider)) {
			return new Properties();
		}
		return ((StatisticsProvider) source).getStatistics();
	}

	public void skip() {
		if (source instanceof Skippable) {
			((Skippable)source).skip();
		}
	}

}
