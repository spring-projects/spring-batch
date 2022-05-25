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
import java.util.Date;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

/**
 * Domain representation of a parameter to a batch job. Only the following types can be
 * parameters: String, Long, Date, and Double. The identifying flag is used to indicate if
 * the parameter is to be used as part of the identification of a job instance.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 2.0
 *
 */
public class JobParameter implements Serializable {

	private final Object parameter;

	private final ParameterType parameterType;

	private final boolean identifying;

	/**
	 * Construct a new {@code JobParameter} from a {@link String}.
	 * @param parameter {@link String} instance. Must not be {@code null}.
	 * @param identifying {@code true} if the {@code JobParameter} should be identifying.
	 */
	public JobParameter(@NonNull String parameter, boolean identifying) {
		this(parameter, identifying, ParameterType.STRING);
	}

	/**
	 * Construct a new {@code JobParameter} from a {@link Long}.
	 * @param parameter {@link Long} instance. Must not be {@code null}.
	 * @param identifying {@code true} if the {@code JobParameter} should be identifying.
	 */
	public JobParameter(@NonNull Long parameter, boolean identifying) {
		this(parameter, identifying, ParameterType.LONG);
	}

	/**
	 * Construct a new {@code JobParameter} from a {@link Date}.
	 * @param parameter {@link Date} instance. Must not be {@code null}.
	 * @param identifying {@code true} if the {@code JobParameter} should be identifying.
	 */
	public JobParameter(@NonNull Date parameter, boolean identifying) {
		this(parameter, identifying, ParameterType.DATE);
	}

	/**
	 * Construct a new {@code JobParameter} from a {@link Double}.
	 * @param parameter {@link Double} instance. Must not be {@code null}.
	 * @param identifying {@code true} if the {@code JobParameter} should be identifying.
	 */
	public JobParameter(@NonNull Double parameter, boolean identifying) {
		this(parameter, identifying, ParameterType.DOUBLE);
	}

	private JobParameter(Object parameter, boolean identifying, ParameterType parameterType) {
		Assert.notNull(parameter, "parameter must not be null");
		this.parameter = parameter;
		this.parameterType = parameterType;
		this.identifying = identifying;
	}

	/**
	 * Construct a new {@code JobParameter} from a {@link String}.
	 * @param parameter A {@link String} instance.
	 */
	public JobParameter(String parameter) {
		this(parameter, true);
	}

	/**
	 * Construct a new {@code JobParameter} from a {@link Long}.
	 * @param parameter A {@link Long} instance.
	 */
	public JobParameter(Long parameter) {
		this(parameter, true);
	}

	/**
	 * Construct a new {@code JobParameter} as a {@link Date}.
	 * @param parameter A {@link Date} instance.
	 */
	public JobParameter(Date parameter) {
		this(parameter, true);
	}

	/**
	 * Construct a new {@code JobParameter} from a {@link Double}.
	 * @param parameter A {@link Double} instance.
	 */
	public JobParameter(Double parameter) {
		this(parameter, true);
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
	public Object getValue() {
		return parameter;
	}

	/**
	 * @return a {@link ParameterType} representing the type of this parameter.
	 */
	public ParameterType getType() {
		return parameterType;
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
		return parameterType == rhs.parameterType && parameter.equals(rhs.parameter);
	}

	@Override
	public String toString() {
		return parameterType == ParameterType.DATE ? "" + ((Date) parameter).getTime() : parameter.toString();
	}

	@Override
	public int hashCode() {
		return 7 + 21 * parameter.hashCode();
	}

	/**
	 * Enumeration representing the type of {@link JobParameter}.
	 */
	public enum ParameterType {

		/**
		 * String parameter type.
		 */
		STRING,
		/**
		 * Date parameter type.
		 */
		DATE,
		/**
		 * Long parameter type.
		 */
		LONG,
		/**
		 * Double parameter type.
		 */
		DOUBLE;

	}

}
