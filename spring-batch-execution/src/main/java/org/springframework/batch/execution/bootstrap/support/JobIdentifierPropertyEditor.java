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

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.runtime.JobIdentifierFactory;
import org.springframework.batch.core.runtime.SimpleJobIdentifierFactory;

/**
 * Simple adapter for a {@link JobIdentifierFactory} that can be used to convert
 * from a {@link Job} name to a {@link JobIdentifier}.
 * 
 * @author Dave Syer
 * 
 */
public class JobIdentifierPropertyEditor extends PropertyEditorSupport {

	private JobIdentifierFactory jobIdentifierFactory = new SimpleJobIdentifierFactory();

	/**
	 * Public setter for the {@link JobIdentifierFactory}.
	 * @param jobIdentifierFactory the jobIdentifierFactory to set
	 */
	public void setJobIdentifierFactory(JobIdentifierFactory jobIdentifierFactory) {
		this.jobIdentifierFactory = jobIdentifierFactory;
	}

	/**
	 * Accept name of {@link Job} and create a {@link JobIdentifier}.
	 * 
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(jobIdentifierFactory.getJobIdentifier(text));
	}

	/**
	 * Extract the name from the {@link JobIdentifier}.
	 * 
	 * @see java.beans.PropertyEditorSupport#getAsText()
	 */
	public String getAsText() {
		JobIdentifier identifier = (JobIdentifier) getValue();
		if (identifier == null) {
			return null;
		}
		return identifier.getName();
	}
}