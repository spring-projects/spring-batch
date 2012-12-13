/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Domain representation of a parameter to a batch job. Only the following types
 * can be parameters: String, Long, Date, and Double.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @since 2.0
 *
 */
public class JobParameter implements Serializable {

	private final Object parameter;

	private final ParameterType parameterType;

	private final boolean identifying;

	/**
	 * Construct a new JobParameter as a String.
	 */
	public JobParameter(String parameter) {
		this(parameter, ParameterType.STRING, true);
	}

	/**
	 * Construct a new JobParameter as a Long.
	 *
	 * @param parameter
	 */
	public JobParameter(Long parameter) {
		this(parameter, ParameterType.LONG, true);
	}

	/**
	 * Construct a new JobParameter as a Date.
	 *
	 * @param parameter
	 */
	public JobParameter(Date parameter) {
		this(parameter, ParameterType.DATE, true);
	}

	/**
	 * Construct a new JobParameter as a Double.
	 *
	 * @param parameter
	 */
	public JobParameter(Double parameter) {
		this(parameter, ParameterType.DOUBLE, true);
	}

	/**
	 * Construct a new JobParameter as a String,
	 * with identifying flag
	 */
	public JobParameter(String parameter, boolean identifying) {
		this(parameter, ParameterType.STRING, identifying);
	}

	/**
	 * Construct a new JobParameter as a Long,
	 * with identifying flag
	 *
	 * @param parameter
	 */
	public JobParameter(Long parameter, boolean identifying) {
		this(parameter, ParameterType.LONG, identifying);
	}

	/**
	 * Construct a new JobParameter as a Date,
	 * with identifying flag
	 *
	 * @param parameter
	 */
	public JobParameter(Date parameter, boolean identifying) {
		this(parameter, ParameterType.DATE, identifying);
	}

	/**
	 * Construct a new JobParameter as a Double,
	 * with identifying flag
	 *
	 * @param parameter
	 */
	public JobParameter(Double parameter, boolean identifying) {
		this(parameter, ParameterType.DOUBLE, identifying);
	}

	protected JobParameter(Object parameter,
							ParameterType parameterType,
							boolean identifying) {
		this.parameter = parameter;
		this.parameterType = parameterType;
		this.identifying = identifying;
	}

	/**
	 * @return the value contained within this JobParameter.
	 */
	public Object getValue() {

		if (parameter != null && parameter.getClass().isInstance(Date.class)) {
			return new Date(((Date) parameter).getTime());
		}
		else {
			return parameter;
		}
	}

	/**
	 * @return a ParameterType representing the type of this parameter.
	 */
	public ParameterType getType() {
		return parameterType;
	}

	public boolean isIdentifying() {
		return identifying;
	}

	@Override
	public boolean equals(Object obj) {


		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (! (obj instanceof JobParameter)) {
			return false;
		}

		JobParameter rhs = (JobParameter) obj;
		if (identifying != rhs.identifying) {
			return false;
		}

		if (parameterType != rhs.parameterType) {
			return false;
		}

		if (parameter == null) {
			if (rhs.parameter != null) {
				return false;
			}
		} else if (!parameter.equals(rhs.parameter)) {
			return false;
		}

		return true;
	}

	/**
	 * Create a string presentation for JobParameter.
	 *
	 * Result string will be in the format of
	 * <pre>[prefix]value</pre>
	 *
	 * A <code>(-)</code> prefix will be used if parameter is non-identifying.
	 *
	 * For String, Double and Long parameter, <code>value</code> will be the
	 * result of <code>toString()</code> of the parameter.  For Date, <code>value</code>
	 * will be the time in milliseconds.
	 */
	@Override
	public String toString() {
		if (parameter == null) {
			return null;
		}

		String prefix = this.identifying? "" : "(-)";

		if (parameterType == ParameterType.DATE) {
			return prefix + ((Date) parameter).getTime();
		} else {
			return prefix + parameter;
		}
	}

	@Override
	public int hashCode() {
		int hashCode = 7 + 21 * (parameter == null ? parameterType.hashCode() : parameter.hashCode());

		// Make hashCode backward compatible by only altering hashCode for
		// non-identifying parameters
		if (!identifying) {
			hashCode = hashCode * 21 + 7;
		}

		return hashCode;
	}

	/**
	 * Enumeration representing the type of a JobParameter.
	 */
	public enum ParameterType {

		STRING, DATE, LONG, DOUBLE;
	}
}
