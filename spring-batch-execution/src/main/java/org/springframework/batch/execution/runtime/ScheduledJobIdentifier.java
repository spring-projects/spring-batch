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

import java.util.Date;

import org.springframework.batch.core.domain.JobIdentifier;
import org.springframework.batch.core.domain.JobParametersBuilder;

/**
 * Convenient {@link JobIdentifier} implementation that identifies itself by a
 * {@link Date} as well as an optional String key. The time portion of the
 * schedule date is significant, and clients are responsible for truncating it
 * if it represents a date rather than a timestamp.
 * 
 * @author Dave Syer
 * 
 */
public class ScheduledJobIdentifier extends DefaultJobIdentifier implements JobIdentifier {

	public static final String SCHEDULE_DATE = "schedule.date";

	ScheduledJobIdentifier() {
		this(null);
	}

	public ScheduledJobIdentifier(String name) {
		super(name);
	}

	/**
	 * Convenience constructor that leaves the schedule date null.
	 * 
	 * @param name the name of the job
	 * @param key a unique key for this execution
	 */
	public ScheduledJobIdentifier(String name, String key) {
		super(name, key);
	}

	/**
	 * Convenience constructor that leaves the key null.
	 * 
	 * @param name the name of the job
	 * @param scheduleDate a timestamp
	 */
	public ScheduledJobIdentifier(String name, Date scheduleDate) {
		super(name, new JobParametersBuilder().addDate(SCHEDULE_DATE, scheduleDate).toJobParameters());
	}

	/**
	 * Convenience constructor with all properties.
	 * 
	 * @param name the name of the job
	 * @param key a unique key for this execution
	 * @param scheduleDate a timestamp
	 */
	public ScheduledJobIdentifier(String name, String key, Date scheduleDate) {
		super(name, new JobParametersBuilder().addString(ScheduledJobIdentifier.JOB_KEY, key).addDate(
				SCHEDULE_DATE, scheduleDate).toJobParameters());
	}

	public Date getScheduleDate() {
		return getJobInstanceProperties().getDate(SCHEDULE_DATE);
	}
}
