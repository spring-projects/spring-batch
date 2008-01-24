/**
 * 
 */
package org.springframework.batch.core.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.util.Assert;

/**
 * Helper class for creating {@link JobParameters}. Useful because of all
 * {@link JobParameters} are immutable, and require 3 separate maps of the three
 * supported types to ensure typesafety. Once created, it can be used in the
 * same was a java.lang.StringBuilder (except, order is irrelevant), by adding
 * various parameters types and creating a valid JobRuntimeParametres once
 * finished.
 * 
 * @author Lucas Ward
 * @since 1.0
 * @see JobParameters
 */
public class JobParametersBuilder {

	private final Map stringMap;

	private final Map longMap;

	private final Map dateMap;

	/**
	 * Default constructor. Initializes the builder
	 */
	public JobParametersBuilder() {

		this.stringMap = new HashMap();
		this.longMap = new HashMap();
		this.dateMap = new HashMap();
	}

	/**
	 * Add a new String parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a refernece to this object.
	 */
	public JobParametersBuilder addString(String key, String parameter) {
		Assert.notNull(parameter, "Parameter must not be null.");
		stringMap.put(key, parameter);
		return this;
	}

	/**
	 * Add a new Date parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a refernece to this object.
	 */
	public JobParametersBuilder addDate(String key, Date parameter) {
		Assert.notNull(parameter, "Parameter must not be null.");
		dateMap.put(key, new Date(parameter.getTime()));
		return this;
	}

	/**
	 * Add a new Long parameter for the given key.
	 * 
	 * @param key - parameter accessor.
	 * @param parameter - runtime parameter
	 * @return a refernece to this object.
	 */
	public JobParametersBuilder addLong(String key, Long parameter) {
		Assert.notNull(parameter, "Parameter must not be null.");
		longMap.put(key, parameter);
		return this;
	}

	/**
	 * Conversion method that takes the current state of this builder and
	 * returns it as a JobruntimeParameters object.
	 * 
	 * @return a valid JobRuntimeParameters object.
	 */
	public JobParameters toJobParameters() {
		return new JobParameters(stringMap, longMap, dateMap);
	}
}
