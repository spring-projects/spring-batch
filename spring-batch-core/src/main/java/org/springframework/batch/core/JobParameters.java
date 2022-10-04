/*
 * Copyright 2006-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Value object representing runtime parameters to a batch job. Because the parameters
 * have no individual meaning outside of the {@code JobParameters} object they are
 * contained within, it is a value object rather than an entity. It is also extremely
 * important that a parameters object can be reliably compared to another for equality, in
 * order to determine if one {@code JobParameters} object equals another. Furthermore,
 * because these parameters need to be persisted, it is vital that the types added are
 * restricted.
 *
 * This class is immutable and, therefore, thread-safe.
 *
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 1.0
 */
@SuppressWarnings("serial")
public class JobParameters implements Serializable {

	private final Map<String, JobParameter<?>> parameters;

	/**
	 * Default constructor.
	 */
	public JobParameters() {
		this.parameters = new LinkedHashMap<>();
	}

	/**
	 * Constructor that is initialized with the content of a {@link Map} that contains a
	 * {@code String} key and a {@link JobParameter} value.
	 * @param parameters The {@link Map} that contains a {@code String} key and a
	 * {@link JobParameter} value.
	 */
	public JobParameters(Map<String, JobParameter<?>> parameters) {
		this.parameters = new LinkedHashMap<>(parameters);
	}

	/**
	 * Typesafe getter for the {@link Long} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return The {@link Long} value or {@code null} if the key is absent.
	 */
	@Nullable
	public Long getLong(String key) {
		if (!parameters.containsKey(key)) {
			return null;
		}
		JobParameter<?> jobParameter = parameters.get(key);
		if (!jobParameter.getType().equals(Long.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type Long");
		}
		return (Long) jobParameter.getValue();
	}

	/**
	 * Typesafe getter for the {@link Long} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	@Nullable
	public Long getLong(String key, @Nullable Long defaultValue) {
		if (parameters.containsKey(key)) {
			return getLong(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link String} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return The {@link String} value or {@code null} if the key is absent.
	 */
	@Nullable
	public String getString(String key) {
		if (!parameters.containsKey(key)) {
			return null;
		}
		JobParameter<?> jobParameter = parameters.get(key);
		if (!jobParameter.getType().equals(String.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type String");
		}
		return (String) jobParameter.getValue();
	}

	/**
	 * Typesafe getter for the {@link String} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The defult value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	@Nullable
	public String getString(String key, @Nullable String defaultValue) {
		if (parameters.containsKey(key)) {
			return getString(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link Long} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return The {@link Double} value or {@code null} if the key is absent.
	 */
	@Nullable
	public Double getDouble(String key) {
		if (!parameters.containsKey(key)) {
			return null;
		}
		JobParameter<?> jobParameter = parameters.get(key);
		if (!jobParameter.getType().equals(Double.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type Double");
		}
		return (Double) jobParameter.getValue();
	}

	/**
	 * Typesafe getter for the {@link Double} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	@Nullable
	public Double getDouble(String key, @Nullable Double defaultValue) {
		if (parameters.containsKey(key)) {
			return getDouble(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link Date} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return the {@link java.util.Date} value or {@code null} if the key is absent.
	 */
	@Nullable
	public Date getDate(String key) {
		if (!parameters.containsKey(key)) {
			return null;
		}
		JobParameter<?> jobParameter = parameters.get(key);
		if (!jobParameter.getType().equals(Date.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type java.util.Date");
		}
		return (Date) jobParameter.getValue();
	}

	/**
	 * Typesafe getter for the {@link Date} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	@Nullable
	public Date getDate(String key, @Nullable Date defaultValue) {
		if (parameters.containsKey(key)) {
			return getDate(key);
		}
		else {
			return defaultValue;
		}
	}

	@Nullable
	public JobParameter<?> getParameter(String key) {
		Assert.notNull(key, "key must not be null");
		return parameters.get(key);
	}

	/**
	 * Get a map of all parameters.
	 * @return an unmodifiable map containing all parameters.
	 */
	public Map<String, JobParameter<?>> getParameters() {
		return Collections.unmodifiableMap(parameters);
	}

	/**
	 * @return {@code true} if the parameters object is empty or {@code false} otherwise.
	 */
	public boolean isEmpty() {
		return parameters.isEmpty();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JobParameters == false) {
			return false;
		}

		if (obj == this) {
			return true;
		}

		JobParameters rhs = (JobParameters) obj;
		return this.parameters.equals(rhs.parameters);
	}

	@Override
	public int hashCode() {
		return 17 + 23 * parameters.hashCode();
	}

	@Override
	public String toString() {
		List<String> parameters = new ArrayList<>();
		for (Map.Entry<String, JobParameter<?>> entry : this.parameters.entrySet()) {
			parameters.add(String.format("'%s':'%s'", entry.getKey(), entry.getValue()));
		}
		return new StringBuilder("{").append(String.join(",", parameters)).append("}").toString();
	}

	/**
	 * @return The {@link Properties} that contain the key and values for the
	 * {@link JobParameter} objects.
	 * @deprecated since 5.0, scheduled for removal in 5.2. Use
	 * {@link org.springframework.batch.core.converter.JobParametersConverter#getProperties(JobParameters)}
	 *
	 */
	@Deprecated(since = "5.0", forRemoval = true)
	public Properties toProperties() {
		Properties props = new Properties();

		for (Map.Entry<String, JobParameter<?>> param : parameters.entrySet()) {
			if (param.getValue() != null) {
				props.put(param.getKey(), Objects.toString(param.getValue().toString(), ""));
			}
		}

		return props;
	}

}
