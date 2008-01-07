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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
public class ScheduledJobIdentifierFactory extends DefaultJobIdentifierFactory implements JobIdentifierFactory {

	private Date scheduleDate;
	
	private DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	public JobIdentifier getJobIdentifier(String name) {

		initDate();
		ScheduledJobIdentifier identifier = new ScheduledJobIdentifier(name, key, scheduleDate);
		return identifier;
	}

	public void setScheduleDate(Date scheduleDate) {
		this.scheduleDate = scheduleDate;
	}

	public void setDateFormat(DateFormat dateFormat){
		this.dateFormat = dateFormat;
	}
	
	private void initDate() {
		try {
			scheduleDate = dateFormat.parse("19700101");
		} catch (ParseException e) {
			throw new IllegalStateException("Could not parse trivial date 19700101");
		}
	}
}
