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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.batch.core.runtime.JobIdentifier;
import org.springframework.batch.core.runtime.SimpleJobIdentifier;

public class ScheduledJobIdentifier extends SimpleJobIdentifier implements JobIdentifier {

	private Date scheduleDate = new Date(0);

	private int jobRun = 0;

	private String jobStream = "";

	ScheduledJobIdentifier() {}
	
	public ScheduledJobIdentifier(String name) {
		super(name);
	}

	public int getJobRun() {
		return jobRun;
	}

	public void setJobRun(int jobRun) {
		this.jobRun = jobRun;
	}

	public String getJobStream() {
		return jobStream;
	}

	public void setJobStream(String jobStream) {
		this.jobStream = jobStream;
	}

	public Date getScheduleDate() {
		return scheduleDate;
	}

	public void setScheduleDate(Date scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

	public String toString() {

		return super.toString() + ",stream=" + jobStream + ",run=" + jobRun + ",scheduleDate="
				+ scheduleDate;
	}

	/**
	 * Returns true if the provided JobIdentifier equals this JobIdentifier. Two
	 * Identifiers are considered to be equal if they have the same name,
	 * stream, run, and schedule date.
	 */
	public boolean equals(Object other) {
		return EqualsBuilder.reflectionEquals(this, other);
	}
	
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
	}

}
