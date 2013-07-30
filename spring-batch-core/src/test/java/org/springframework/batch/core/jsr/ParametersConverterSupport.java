package org.springframework.batch.core.jsr;

import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

public class ParametersConverterSupport implements ParametersConverter {

	@Override
	public JobParameters convert(Properties parameters) {
		JobParametersBuilder builder = new JobParametersBuilder();

		if(parameters != null) {
			for (Map.Entry<Object, Object> curParameter : parameters.entrySet()) {
				if(curParameter.getValue() != null) {

					builder.addString(curParameter.getKey().toString(), curParameter.getValue().toString(), false);
				}
			}
		}

		return builder.toJobParameters();
	}

	@Override
	public Properties convert(JobParameters parameters) {
		Properties properties = new Properties();

		if(properties != null) {
			for(Map.Entry<String, JobParameter> curParameter: parameters.getParameters().entrySet()) {
				properties.setProperty(curParameter.getKey(), curParameter.getValue().getValue().toString());
			}
		}

		return properties;
	}
}
