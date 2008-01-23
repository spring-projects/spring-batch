/*
 * Copyright 2006-2008 the original author or authors.
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map.Entry;

import org.springframework.batch.core.domain.JobInstanceProperties;
import org.springframework.batch.core.domain.JobInstancePropertiesBuilder;
import org.springframework.batch.core.domain.JobInstancePropertiesFactory;
import org.springframework.util.Assert;

/**
 * @author Lucas Ward
 *
 */
public class ScheduledJobInstancePropertiesFactory implements
		JobInstancePropertiesFactory {

	public static String SCHEDULE_DATE_KEY = "schedule.date";
	public static String JOB_KEY = "job.key";
	
	private DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
	
	/* (non-Javadoc)
	 * @see org.springframework.batch.core.domain.JobInstancePropertiesFactory#getProperties(java.lang.String[])
	 */
	public JobInstanceProperties getProperties(String[] args) {
		
		Assert.notNull(args, "Factory arguments must not be null.");
		
		JobInstancePropertiesBuilder propertiesBuilder = new JobInstancePropertiesBuilder();

		Properties props = parseArgs(args);
		
		for(Iterator it = props.entrySet().iterator(); it.hasNext();){
			Entry entry = (Entry)it.next();
			if(entry.getKey().equals(SCHEDULE_DATE_KEY)){
				Date scheduleDate;
				try{
					scheduleDate = dateFormat.parse(entry.getValue().toString());
				}
				catch(ParseException ex){
					throw new IllegalArgumentException("Schedule date format is invalid: [" + entry.getValue() + "]", ex);
				}
				propertiesBuilder.addDate(entry.getKey().toString(), scheduleDate);
			}
			else{
				propertiesBuilder.addString(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		
		return propertiesBuilder.toJobParameters();
	}
	
	private Properties parseArgs(String[] args){
		Properties props = new Properties();
		
		for(int i = 0; i < args.length; i++){
			String property = args[i];
			int equalsIndex = property.indexOf('=');
			
			if(equalsIndex == -1){
				throw new IllegalArgumentException("JobInstacePropertes argument invalid: [" + property + "]");
			}
			String key = property.substring(0, equalsIndex);
			props.put(key, property.substring(equalsIndex + 1));
		}
		
		return props;
	}

	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}
}
