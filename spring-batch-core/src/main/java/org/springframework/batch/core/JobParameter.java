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

	/**
	 * Construct a new JobParameter as a String.
	 */
	public JobParameter(String parameter) {
		this.parameter = parameter;
		parameterType = ParameterType.STRING;
	}

	/**
	 * Construct a new JobParameter as a Long.
	 * 
	 * @param parameter
	 */
	public JobParameter(Long parameter) {
		this.parameter = parameter;
		parameterType = ParameterType.LONG;
	}

	/**
	 * Construct a new JobParameter as a Date.
	 * 
	 * @param parameter
	 */
	public JobParameter(Date parameter) {
		this.parameter = parameter;
		parameterType = ParameterType.DATE;
	}

	/**
	 * Construct a new JobParameter as a Double.
	 * 
	 * @param parameter
	 */
	public JobParameter(Double parameter) {
		this.parameter = parameter;
		parameterType = ParameterType.DOUBLE;
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

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof JobParameter == false) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		JobParameter rhs = (JobParameter) obj;
		return parameter==null ? rhs.parameter==null && parameterType==rhs.parameterType: parameter.equals(rhs.parameter);
	}

	@Override
	public String toString() {
		return parameter == null ? null : (parameterType == ParameterType.DATE ? "" + ((Date) parameter).getTime()
				: parameter.toString());
	}

	public int hashCode() {
		return 7 + 21 * (parameter == null ? parameterType.hashCode() : parameter.hashCode());
	}

	/**
	 * Enumeration representing the type of a JobParameter.
	 */
	public enum ParameterType {

		STRING, DATE, LONG, DOUBLE;
	}
}
