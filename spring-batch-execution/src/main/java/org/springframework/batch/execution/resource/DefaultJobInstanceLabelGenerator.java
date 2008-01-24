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
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.springframework.batch.core.domain.JobInstance;
import org.springframework.batch.core.domain.JobParameters;

/**
 * {@link JobInstanceLabelGenerator} that knows about {@link JobParameters} and
 * provides a fixed format label for each.
 * 
 * 
 * @author Dave Syer
 * 
 */
public class DefaultJobInstanceLabelGenerator implements JobInstanceLabelGenerator {

	private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

	/**
	 * Concatenate the properties of the {@link JobInstance}. From an instance
	 * with no additional parameters we just get the name, otherwise we get the
	 * parameters joined by hyphens (Strings then Longs then Dates). The date
	 * format is "yyyyMMdd".
	 * 
	 * @see org.springframework.batch.execution.resource.JobInstanceLabelGenerator#getLabel(JobInstance)
	 */
	public String getLabel(JobInstance jobInstance) {
		if (jobInstance == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder(""+jobInstance.getJobName());
		Map map = jobInstance.getJobInstanceProperties().getParameters();
		for (Iterator iterator = map.values().iterator(); iterator.hasNext();) {
			Object value = iterator.next();
			builder.append("-" + ((value instanceof Date) ? dateFormat.format(value) : ""+value));
		}
		return builder.toString();
	}
}
