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

import java.beans.PropertyEditorSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.JobParametersBuilder;
import org.springframework.util.StringUtils;

/**
 * Factory for {@link JobParameters} instances using a simple naming convention
 * for property keys. Key names ending with "(&lt;type&gt;)" where type is one
 * of string, date, long are converted to the corresponding type. The default
 * type is string. E.g.
 * 
 * <pre>
 * schedule.date(date)=2007/12/11
 * department.id(long)=2345
 * </pre>
 * 
 * The literal values are converted to the correct type using the default Spring
 * strategies, augmented if necessary by the custom editors provided. 
 * 
 * TODO: finish this (only supports Strings so far).
 * 
 * @author Dave Syer
 * 
 */
public class JobParametersPropertyEditor extends PropertyEditorSupport {

	/**
	 * Accept properties in the form of name=value pairs, delimited by either
	 * comma or new line (or both) and create {@link JobParameters}.
	 * 
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		JobParametersBuilder builder = new JobParametersBuilder();
		Properties properties = StringUtils.splitArrayElementsIntoProperties(StringUtils.tokenizeToStringArray(text,
				",\n"), "=");
		for (Iterator iterator = properties.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			key = StringUtils.tokenizeToStringArray(key, "(")[0];
			builder.addString(key, properties.getProperty(key));
		}
		setValue(builder.toJobParameters());
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
		List builder = new ArrayList();
		Map map = params.getStringParameters();
		for (Iterator iterator = map.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			builder.add(key+"="+map.get(key));
		}
		return StringUtils.collectionToCommaDelimitedString(builder);
	}
}