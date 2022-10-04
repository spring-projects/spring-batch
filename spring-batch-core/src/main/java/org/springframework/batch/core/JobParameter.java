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

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Domain representation of a parameter to a batch job. The identifying flag is used to
 * indicate if the parameter is to be used as part of the identification of a job
 * instance.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.0
 *
 */
public class JobParameter<T> implements Serializable {

	private T value;

	private Class<T> type;

	private boolean identifying;

	/**
	 * reate a new {@link JobParameter}.
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter. Must not be {@code null}.
	 * @param identifying true if the parameter is identifying. false otherwise.
	 */
	public JobParameter(@NonNull T value, @NonNull Class<T> type, boolean identifying) {
		Assert.notNull(value, "value must not be null");
		Assert.notNull(value, "type must not be null");
		this.value = value;
		this.type = type;
		this.identifying = identifying;
	}

	/**
	 * Create a new identifying {@link JobParameter}.
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter. Must not be {@code null}.
	 */
	public JobParameter(@NonNull T value, @NonNull Class<T> type) {
		this(value, type, true);
	}

	/**
	 * @return The identifying flag. It is set to {@code true} if the job parameter is
	 * identifying.
	 */
	public boolean isIdentifying() {
		return identifying;
	}

	/**
	 * @return the value contained within this {@code JobParameter}.
	 */
	public T getValue() {
		return value;
	}

	/**
	 * Return the type of the parameter.
	 * @return the type of the parameter
	 */
	public Class<T> getType() {
		return type;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof JobParameter)) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		JobParameter rhs = (JobParameter) obj;
		return type == rhs.type && value.equals(rhs.value);
	}

	@Override
	public String toString() {
		return "{" + "value=" + value + ", type=" + type + ", identifying=" + identifying + '}';
	}

	@Override
	public int hashCode() {
		return 7 + 21 * value.hashCode();
	}

}
