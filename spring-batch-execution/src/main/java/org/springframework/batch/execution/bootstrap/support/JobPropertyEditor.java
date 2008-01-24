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

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobLocator;
import org.springframework.batch.core.domain.NoSuchJobException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link PropertyEditor} that delegates to a {@link JobLocator}.
 * @author Dave Syer
 * 
 */
public class JobPropertyEditor extends PropertyEditorSupport implements InitializingBean {

	private JobLocator jobLocator;
	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(jobLocator, "JobLocator is required");
	}

	/**
	 * Accept job name and convert to {@link Job} through the injected {@link JobLocator}.
	 * 
	 * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
	 */
	public void setAsText(String text) throws IllegalArgumentException {
		try {
			setValue(jobLocator.getJob(text));
		}
		catch (NoSuchJobException e) {
			throw new IllegalArgumentException(e);
		}
	}

	/**
	 * Extract the name from the {@link JobIdentifier}.
	 * 
	 * @see java.beans.PropertyEditorSupport#getAsText()
	 */
	public String getAsText() {
		Job job = (Job) getValue();
		if (job == null) {
			return null;
		}
		return job.getName();
	}

	/**
	 * Public setter for the {@link JobLocator}.
	 * @param jobLocator the jobLocator to set
	 */
	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}
}