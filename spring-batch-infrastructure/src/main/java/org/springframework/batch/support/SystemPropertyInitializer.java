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
package org.springframework.batch.support;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * Helper class that sets up a System property with a default value. A System
 * property is created with the specified key name, and default value (i.e. if
 * the property already exists it is not changed).
 * 
 * @author Dave Syer
 * 
 */
public class SystemPropertyInitializer implements InitializingBean {

	/**
	 * Name of system property used by default.
	 */
	public static final String ENVIRONMENT = "org.springframework.batch.support.SystemPropertyInitializer.ENVIRONMENT";

	private String keyName = ENVIRONMENT;

	private String defaultValue;

	/**
	 * Set the key name for the System property that is created. Defaults to
	 * {@link #ENVIRONMENT}.
	 * 
	 * @param keyName the key name to set
	 */
	public void setKeyName(String keyName) {
		this.keyName = keyName;
	}

	/**
	 * Mandatory property specifying the default value of the System property.
	 * 
	 * @param defaultValue the default value to set
	 */
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * Sets the System property with the provided name and default value.
	 * 
	 * @see InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.state(defaultValue != null || System.getProperty(keyName) != null,
				"Either a default value must be specified or the value should already be set for System property: "
						+ keyName);
		System.setProperty(keyName, System.getProperty(keyName, defaultValue));
	}

}
