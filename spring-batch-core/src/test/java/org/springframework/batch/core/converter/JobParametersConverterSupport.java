package org.springframework.batch.core.converter;

import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class JobParametersConverterSupport implements JobParametersConverter {

	@Override
	public JobParameters getJobParameters(Properties properties) {
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
	public Properties getProperties(JobParameters params) {
		Properties properties = new Properties();

		if(params != null) {
			for(Map.Entry<String, JobParameter> curParameter: params.getParameters().entrySet()) {
				properties.setProperty(curParameter.getKey(), curParameter.getValue().getValue().toString());
			}
		}

		return properties;
	}
}
