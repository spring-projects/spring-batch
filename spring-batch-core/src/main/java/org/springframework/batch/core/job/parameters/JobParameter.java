/*
 * Copyright 2006-2023 the original author or authors.
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
 * @author Song JaeGeun
 * @since 2.0
 *
 */
public class JobParameter<T> implements Serializable {

	private final T value;

	private final Class<T> type;

	private final boolean identifying;

	/**
	 * Create a new {@link JobParameter}.
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter. Must not be {@code null}.
	 * @param identifying true if the parameter is identifying. false otherwise.
	 */
	public JobParameter(T value, Class<T> type, boolean identifying) {
		Assert.notNull(value, "value must not be null");
		Assert.notNull(type, "type must not be null");
		this.value = value;
		this.type = type;
		this.identifying = identifying;
	}

	/**
	 * Create a new identifying {@link JobParameter}.
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter. Must not be {@code null}.
	 */
	public JobParameter(T value, Class<T> type) {
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
		if (!(obj instanceof JobParameter rhs)) {
			return false;
		}

		if (this == obj) {
			return true;
		}

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
