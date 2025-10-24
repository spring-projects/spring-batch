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
import java.util.Objects;

import org.springframework.util.Assert;

/**
 * Domain representation of a parameter to a batch job. The identifying flag is used to
 * indicate if the parameter is to be used as part of the identification of a job
 * instance. A job parameter only has a meaning within a {@link JobParameters} instance,
 * which is a namespace of job parameters with unique names. Two job parameters are
 * considered equal if they have the same name. Job parameters are immutable.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @author Song JaeGeun
 * @since 2.0
 *
 */
public record JobParameter<T>(String name, T value, Class<T> type, boolean identifying) implements Serializable {

	/**
	 * Create a new {@link JobParameter}.
	 * @param name the name of the parameter. Must not be {@code null}.
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter. Must not be {@code null}.
	 * @param identifying true if the parameter is identifying. false otherwise.
	 * @since 6.0
	 */
	public JobParameter {
		Assert.notNull(name, "name must not be null");
		Assert.notNull(value, "value must not be null");
		Assert.notNull(type, "type must not be null");
	}

	/**
	 * Create a new identifying {@link JobParameter}.
	 * @param name the name of the parameter. Must not be {@code null}.
	 * @param value the value of the parameter. Must not be {@code null}.
	 * @param type the type of the parameter. Must not be {@code null}.
	 * @since 6.0
	 */
	public JobParameter(String name, T value, Class<T> type) {
		this(name, value, type, true);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof JobParameter<?> that))
			return false;
		return Objects.equals(name, that.name);
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(name);
	}

	@Override
	public String toString() {
		return "JobParameter{" + "name='" + name + '\'' + ", value=" + value + ", type=" + type + ", identifying="
				+ identifying + '}';
	}

}
