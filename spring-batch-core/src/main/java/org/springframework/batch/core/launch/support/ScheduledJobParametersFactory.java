/*
 * Copyright 2006-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.core.launch.support;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.lang.Nullable;

/**
 * @author Lucas Ward
 * @author Mahmoud Ben Hassine
 *
 * @deprecated as of v4.3 in favor of 
 * {@link org.springframework.batch.core.converter.DefaultJobParametersConverter}
 * and scheduled for removal in v5.0.
 */
@Deprecated
public class ScheduledJobParametersFactory implements JobParametersConverter {

	public static final String SCHEDULE_DATE_KEY = "schedule.date";

	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.runtime.JobParametersFactory#getJobParameters(java.util.Properties)
	 */
	@Override
	public JobParameters getJobParameters(@Nullable Properties props) {

		if (props == null || props.isEmpty()) {
			return new JobParameters();
		}

		JobParametersBuilder propertiesBuilder = new JobParametersBuilder();

		for (Entry<Object, Object> entry : props.entrySet()) {
			if (entry.getKey().equals(SCHEDULE_DATE_KEY)) {
				Date scheduleDate;
				try {
					scheduleDate = dateFormat.parse(entry.getValue().toString());
				} catch (ParseException ex) {
					throw new IllegalArgumentException("Date format is invalid: [" + entry.getValue() + "]");
				}
				propertiesBuilder.addDate(entry.getKey().toString(), scheduleDate);
			} else {
				propertiesBuilder.addString(entry.getKey().toString(), entry.getValue().toString());
			}
		}

		return propertiesBuilder.toJobParameters();
	}

	/**
	 * Convert schedule date to Date, and assume all other parameters can be represented by their default string value.
	 *
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getProperties(org.springframework.batch.core.JobParameters)
	 */
	@Override
	public Properties getProperties(@Nullable JobParameters params) {

		if (params == null || params.isEmpty()) {
			return new Properties();
		}

		Map<String, JobParameter> parameters = params.getParameters();
		Properties result = new Properties();
		for (Entry<String, JobParameter> entry : parameters.entrySet()) {
			String key = entry.getKey();
			JobParameter jobParameter = entry.getValue();
			if (key.equals(SCHEDULE_DATE_KEY)) {
				result.setProperty(key, dateFormat.format(jobParameter.getValue()));
			} else {
				result.setProperty(key, "" + jobParameter.getValue());
			}
		}
		return result;
	}

	/**
	 * Public setter for injecting a date format.
	 *
	 * @param dateFormat a {@link DateFormat}, defaults to "yyyy/MM/dd"
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}
}
