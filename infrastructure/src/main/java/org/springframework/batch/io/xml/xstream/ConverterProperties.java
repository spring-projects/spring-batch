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

package org.springframework.batch.io.xml.xstream;

import com.thoughtworks.xstream.XStream;

/**
 * Represents converter to be registered for parsing. Converter acts as a
 * strategy for converting a particular type of class to XML and back again.
 * 
 * @author peter.zozom
 */

public class ConverterProperties {
	private String className;

	private int priority = XStream.PRIORITY_NORMAL;

	/**
	 * @return converter class name
	 */
	protected String getClassName() {
		return className;
	}

	/**
	 * Set converter class name.
	 * @param className converter class name
	 */
	protected void setClassName(String className) {
		this.className = className;
	}

	/**
	 * @return converter priority
	 */
	protected int getPriority() {
		return priority;
	}

	/**
	 * @param priority converter priority
	 */
	protected void setPriority(int priority) {
		this.priority = priority;
	}
}
