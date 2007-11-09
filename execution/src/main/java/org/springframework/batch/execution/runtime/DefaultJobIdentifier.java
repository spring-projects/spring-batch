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
package org.springframework.batch.execution.runtime;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

/**
 * @author Dave Syer
 *
 */
public class DefaultJobIdentifier extends SimpleJobIdentifier implements
		JobIdentifier {

	private String key = "";

	/**
	 * Default constructor package access only.
	 */
	DefaultJobIdentifier() {
		this(null);
	}

	/**
	 * @param name the name for the job
	 */
	public DefaultJobIdentifier(String name) {
		super(name);
	}

	public String getJobKey() {
		return key;
	}

	public void setJobKey(String key) {
		this.key = key;
	}

	
	/**
	 * Adds the key data to the base class.
	 * 
	 * @see org.springframework.batch.core.runtime.SimpleJobIdentifier#toString()
	 */
	public String toString() {
		return super.toString() + ",key=" + key;
	}
	
	/**
	 * Returns true if the provided JobIdentifier equals this JobIdentifier. Two
	 * Identifiers are considered to be equal if they have the same name,
	 * stream, run, and schedule date.
	 */
	public boolean equals(Object other) {
		return EqualsBuilder.reflectionEquals(this, other) || EqualsBuilder.reflectionEquals(other, this);
	}
	
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}
	
}
