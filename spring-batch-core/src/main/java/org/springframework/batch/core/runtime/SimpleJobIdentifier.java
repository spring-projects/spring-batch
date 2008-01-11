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
package org.springframework.batch.core.runtime;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.util.ClassUtils;


/**
 * Simple, immutable, implementation of the JobIdentifier interface.  
 * 
 * @author Dave Syer
 * @author Lucas Ward
 *
 */
public class SimpleJobIdentifier implements JobIdentifier {

	private String name;
	private JobInstanceProperties runtimeParameters;
	
	/**
	 * Default constructor.  Since there it is required that the Identifier at least have a name,
	 * the default constructor should not be called.
	 */
	private SimpleJobIdentifier() {
		super();
	}
	
	/**
	 * Convenience constructor with name.
	 * @param name
	 */
	public SimpleJobIdentifier(String name) {
		this(name, new JobInstanceProperties());
	}
	
	public SimpleJobIdentifier(String name, JobInstanceProperties runtimeParameters){
		this.name = name;
		this.runtimeParameters = runtimeParameters;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.runtime.JobIdentifier#getName()
	 */
	public String getName() {
		return this.name;
	}
	
	public JobInstanceProperties getRuntimeParameters() {
		return runtimeParameters;
	}
	
	public String toString() {
		return ClassUtils.getShortName(getClass())+": name=" + name + "parameters=" + runtimeParameters;
	}

	public boolean equals(Object obj) {
		
		if(obj instanceof SimpleJobIdentifier == false){
			return false;
		}
		
		if(this == obj){
			return true;
		}
		
		SimpleJobIdentifier rhs = (SimpleJobIdentifier)obj;
		return new EqualsBuilder().
								append(runtimeParameters,rhs.getRuntimeParameters()).
								append(name, rhs.getName()).
								isEquals();
	}
	
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

}
