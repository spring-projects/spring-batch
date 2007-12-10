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
package org.springframework.batch.execution.resource;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;
import org.springframework.batch.execution.runtime.ScheduledJobIdentifier;

/**
 * {@link JobIdentifierLabelGenerator} that knows about
 * {@link SimpleJobIdentifier} and {@link ScheduledJobIdentifier} and provides a
 * fixed format label for each.
 * 
 * 
 * @author Dave Syer
 * 
 */
public class DefaultJobIdentifierLabelGenerator implements
		JobIdentifierLabelGenerator {

	private static final DateFormat dateFormat = new SimpleDateFormat(
			"yyyyMMdd");

	/**
	 * Concatenate the properties of the {@link JobIdentifier}. From a
	 * {@link SimpleJobIdentifier} we just get the name, and from a
	 * {@link ScheduledJobIdentifier} we get the name, stream, run and schedule
	 * date (yyyyMMdd) joined by hyphens.
	 * 
	 * @see org.springframework.batch.execution.resource.JobIdentifierLabelGenerator#getLabel(org.springframework.batch.core.domain.JobIdentifier)
	 */
	public String getLabel(JobIdentifier jobIdentifier) {
		if (jobIdentifier == null) {
			return null;
		}
		if (jobIdentifier instanceof ScheduledJobIdentifier) {
			ScheduledJobIdentifier id = (ScheduledJobIdentifier) jobIdentifier;
			return jobIdentifier.getName() + "-" + id.getJobKey() + "-"
					+ dateFormat.format(id.getScheduleDate());
		}
		return jobIdentifier.getName();
	}
}
