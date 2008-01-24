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
package org.springframework.batch.execution.bootstrap.support;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorSupport;
import java.util.Properties;

import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.runtime.JobParametersFactory;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.util.StringUtils;

/**
 * A {@link PropertyEditor} that delegates to a {@link JobParametersFactory}.
 * @author Dave Syer
 * 
 */
public class JobParametersPropertyEditor extends PropertyEditorSupport {

	private JobParametersFactory factory = new DefaultJobParametersFactory();

	/**
	 * Accept properties in the form of name=value pairs, delimited by either
	 * comma or new line (or both) and create {@link JobParameters}.
	 * 
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		Properties properties = StringUtils.splitArrayElementsIntoProperties(StringUtils.tokenizeToStringArray(text,
				",\n"), "=");
		setValue(factory.getJobParameters(properties));
	}

	/**
	 * Extract the name from the {@link JobIdentifier}.
	 * 
	 * @see java.beans.PropertyEditorSupport#getAsText()
	 */
	public String getAsText() {
		JobParameters params = (JobParameters) getValue();
		if (params == null) {
			return null;
		}
		Properties properties = factory.getProperties(params);
		return PropertiesConverter.propertiesToString(properties);
	}
	
	/**
	 * Public setter for the {@link JobParametersFactory}.
	 * @param factory the factory to set
	 */
	public void setFactory(JobParametersFactory factory) {
		this.factory = factory;
	}
}