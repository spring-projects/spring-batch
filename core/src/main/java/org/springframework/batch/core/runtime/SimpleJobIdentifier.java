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


/**
 * @author Dave Syer
 *
 */
public class SimpleJobIdentifier implements JobIdentifier {

	private String name;
	
	
	/**
	 * Default constructor.
	 */
	public SimpleJobIdentifier() {
		super();
	}
	
	/**
	 * Convenience constructor with name.
	 * @param name
	 */
	public SimpleJobIdentifier(String name) {
		super();
		this.name = name;
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.runtime.JobIdentifier#getName()
	 */
	public String getName() {
		return this.name;
	}
	
	/**
	 * Public setter for the name.
	 *
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	public String toString() {

		return "name=" + name;
	}

}
