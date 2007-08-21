package org.springframework.batch.item.processor;

import java.util.Properties;

import org.springframework.batch.io.OutputSource;
import org.springframework.batch.io.Skippable;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.restart.RestartData;
import org.springframework.batch.restart.Restartable;
import org.springframework.batch.statistics.StatisticsProvider;

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
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link Restartable}.
	 */
	public RestartData getRestartData() {
		if (!(source instanceof Restartable)) {
			throw new IllegalStateException("Output Source is not Restartable");
		}
		return ((Restartable) source).getRestartData();
	}

	/**
	 * @see Restartable#restoreFrom(RestartData)
	 * @throws IllegalStateException if the parent template is not itself
	 * {@link Restartable}.
	 */
	public void restoreFrom(RestartData data) {
		if (!(source instanceof Restartable)) {
			throw new IllegalStateException("Output Source is not Restartable");
		}
		((Restartable) source).restoreFrom(data);
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
