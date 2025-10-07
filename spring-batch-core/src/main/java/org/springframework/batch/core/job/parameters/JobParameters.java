/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.core.job.parameters;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

import org.jspecify.annotations.Nullable;

/**
 * Value object representing runtime parameters of a batch job. Because the parameters
 * have no individual meaning outside the {@code JobParameters} object they are contained
 * within, it is a value object rather than an entity. It is also extremely important that
 * a parameters object can be reliably compared to another for equality, in order to
 * determine if one {@code JobParameters} object equals another. This class is a namespace
 * of job parameters and all parameters should have a unique name within that namespace.
 * <p>
 * Furthermore, because these parameters need to be persisted, it is vital that the types
 * added are restricted.
 * <p>
 * This class is immutable and, therefore, thread-safe.
 *
 * @author Lucas Ward
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Taeik Lim
 * @since 1.0
 */
public record JobParameters(Set<JobParameter<?>> parameters) implements Serializable, Iterable<JobParameter<?>> {

	/**
	 * Create a new empty {@link JobParameters} instance.
	 *
	 * @since 6.0
	 */
	//@formatter:off
    // TODO this is questionable (does this even make sense since the class is immutable?),
    // TODO but needed for the incrementer, otherwise we would have incrementer.getNext(null)
    // TODO which is even worse
    //@formatter:on
	public JobParameters() {
		this(new HashSet<>());
	}

	/**
	 * Create a new {@link JobParameters} instance.
	 * @param parameters the set of job parameters, must not be {@code null} or empty
	 * @since 6.0
	 */
	public JobParameters(Set<JobParameter<?>> parameters) {
		Assert.notNull(parameters, "parameters must not be null");
		this.parameters = new HashSet<>(parameters);
	}

	/**
	 * Typesafe getter for the {@link Long} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return The {@link Long} value or {@code null} if the key is absent.
	 */
	public @Nullable Long getLong(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(Long.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type Long");
		}
		return (Long) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link Long} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable Long getLong(String key, Long defaultValue) {
		if (getParameter(key) != null) {
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
	public @Nullable String getString(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(String.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type String");
		}
		return (String) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link String} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The defult value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable String getString(String key, String defaultValue) {
		if (getParameter(key) != null) {
			return getString(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link Double} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return The {@link Double} value or {@code null} if the key is absent.
	 */
	public @Nullable Double getDouble(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(Double.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type Double");
		}
		return (Double) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link Double} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable Double getDouble(String key, Double defaultValue) {
		if (getParameter(key) != null) {
			return getDouble(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link Date} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return the {@link Date} value or {@code null} if the key is absent.
	 */
	public @Nullable Date getDate(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(Date.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type java.util.Date");
		}
		return (Date) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link Date} represented by the provided key. If the key
	 * does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable Date getDate(String key, Date defaultValue) {
		if (getParameter(key) != null) {
			return getDate(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link LocalDate} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return the {@link LocalDate} value or {@code null} if the key is absent.
	 */
	public @Nullable LocalDate getLocalDate(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(LocalDate.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type java.time.LocalDate");
		}
		return (LocalDate) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link LocalDate} represented by the provided key. If the
	 * key does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable LocalDate getLocalDate(String key, LocalDate defaultValue) {
		if (getParameter(key) != null) {
			return getLocalDate(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link LocalTime} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return the {@link LocalTime} value or {@code null} if the key is absent.
	 */
	public @Nullable LocalTime getLocalTime(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(LocalTime.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type java.time.LocalTime");
		}
		return (LocalTime) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link LocalTime} represented by the provided key. If the
	 * key does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable LocalTime getLocalTime(String key, LocalTime defaultValue) {
		if (getParameter(key) != null) {
			return getLocalTime(key);
		}
		else {
			return defaultValue;
		}
	}

	/**
	 * Typesafe getter for the {@link LocalDateTime} represented by the provided key.
	 * @param key The key for which to get a value.
	 * @return the {@link LocalDateTime} value or {@code null} if the key is absent.
	 */
	public @Nullable LocalDateTime getLocalDateTime(String key) {
		JobParameter<?> jobParameter = getParameter(key);
		if (jobParameter == null) {
			return null;
		}
		if (!jobParameter.type().equals(LocalDateTime.class)) {
			throw new IllegalArgumentException("Key " + key + " is not of type java.time.LocalDateTime");
		}
		return (LocalDateTime) jobParameter.value();
	}

	/**
	 * Typesafe getter for the {@link LocalDateTime} represented by the provided key. If
	 * the key does not exist, the default value is returned.
	 * @param key The key for which to return the value.
	 * @param defaultValue The default value to return if the value does not exist.
	 * @return the parameter represented by the provided key or, if that is missing, the
	 * default value.
	 */
	public @Nullable LocalDateTime getLocalDateTime(String key, LocalDateTime defaultValue) {
		if (getParameter(key) != null) {
			return getLocalDateTime(key);
		}
		else {
			return defaultValue;
		}
	}

	public @Nullable JobParameter<?> getParameter(String key) {
		Assert.notNull(key, "key must not be null");
		return this.parameters.stream().filter(parameter -> parameter.name().equals(key)).findFirst().orElse(null);
	}

	/**
	 * Get a set of all parameters.
	 * @return an unmodifiable set containing all parameters.
	 */
	@Override
	public Set<JobParameter<?>> parameters() {
		return Collections.unmodifiableSet(parameters);
	}

	/**
	 * Get a set of identifying parameters.
	 * @return an unmodifiable set containing identifying parameters.
	 * @since 5.1
	 */
	public Set<JobParameter<?>> getIdentifyingParameters() {
		return this.parameters.stream().filter(JobParameter::identifying).collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * @return {@code true} if the parameters object is empty or {@code false} otherwise.
	 */
	public boolean isEmpty() {
		return parameters.isEmpty();
	}

	@Override
	public Iterator<JobParameter<?>> iterator() {
		return parameters.iterator();
	}

	@Override
	public void forEach(Consumer<? super JobParameter<?>> action) {
		Iterable.super.forEach(action);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof JobParameters that))
			return false;
		return Objects.equals(parameters, that.parameters);
	}

	@Override
	public String toString() {
		List<String> parameters = new ArrayList<>();
		for (JobParameter<?> parameter : this.parameters) {
			parameters.add(parameter.toString());
		}
		return "{" + String.join(",", parameters) + "}";
	}

}
