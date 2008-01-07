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
import org.springframework.batch.core.domain.JobRuntimeParametersBuilder;

public class ScheduledJobIdentifier extends DefaultJobIdentifier implements JobIdentifier {
	
	public static final String SCHEDULE_DATE = "schedule.date";
	
	ScheduledJobIdentifier() {
		this(null);
	}
	
	public ScheduledJobIdentifier(String name) {
		super(name);
	}

	/**
	 * @param name
	 * @param key
	 */
	public ScheduledJobIdentifier(String name, String key) {
		super(name, key);
	}
	
	public ScheduledJobIdentifier(String name, Date scheduleDate){
		super(name, new JobRuntimeParametersBuilder().addDate(SCHEDULE_DATE, scheduleDate).toJobRuntimeParameters());
	}
	
	public ScheduledJobIdentifier(String name, String jobKey, Date scheduleDate){
		super(name, new JobRuntimeParametersBuilder().addString(ScheduledJobIdentifier.JOB_KEY, jobKey).
				addDate(SCHEDULE_DATE, scheduleDate).toJobRuntimeParameters());
	}

	public Date getScheduleDate() {
		return getRuntimeParameters().getDate(SCHEDULE_DATE);
	}
}
