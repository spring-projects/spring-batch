/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.converter;

import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.lang.Nullable;

public class JobParametersConverterSupport implements JobParametersConverter {

	@Override
	public JobParameters getJobParameters(@Nullable Properties properties) {
		JobParametersBuilder builder = new JobParametersBuilder();

		if(properties != null) {
			for (Map.Entry<Object, Object> curParameter : properties.entrySet()) {
				if(curParameter.getValue() != null) {
					builder.addString(curParameter.getKey().toString(), curParameter.getValue().toString(), false);
				}
			}
		}

		return builder.toJobParameters();
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getProperties(org.springframework.batch.core.JobParameters)
	 */
	@Override
	public Properties getProperties(@Nullable JobParameters params) {
		Properties properties = new Properties();

		if(params != null) {
			for(Map.Entry<String, JobParameter> curParameter: params.getParameters().entrySet()) {
				properties.setProperty(curParameter.getKey(), curParameter.getValue().getValue().toString());
			}
		}

		return properties;
	}
}
