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
import org.springframework.batch.core.runtime.JobIdentifierFactory;

/**
 * {@link JobIdentifierFactory} for creating {@link ScheduledJobIdentifier}
 * instances.
 * 
 * @author Dave Syer
 * 
 */
public class ScheduledJobIdentifierFactory implements JobIdentifierFactory {

	private String jobStream = "stream";

	private int jobRun = 0;

	private Date scheduleDate = new Date();

	public JobIdentifier getJobIdentifier(String name) {

		ScheduledJobIdentifier runtimeInformation = new ScheduledJobIdentifier(name);
		runtimeInformation.setJobStream(jobStream);
		runtimeInformation.setJobRun(jobRun);
		runtimeInformation.setScheduleDate(scheduleDate);
		return runtimeInformation;
	}

	public void setJobRun(int jobRun) {
		this.jobRun = jobRun;
	}

	public void setJobStream(String jobStream) {
		this.jobStream = jobStream;
	}

	public void setScheduleDate(Date scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

}
