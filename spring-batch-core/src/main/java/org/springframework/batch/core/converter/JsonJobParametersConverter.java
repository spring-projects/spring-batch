/*
 * Copyright 2022 the original author or authors.
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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.util.StringUtils;

/**
 * Converter for {@link JobParameters} instances that uses a JSON naming convention for
 * converting job parameters. The expected notation is the following:
 *
 * key='{"value": "parameterStringLiteralValue",
 * "type":"fully.qualified.name.of.the.parameter.Type", "identifying": "booleanValue"}'
 *
 * where:
 *
 * <ul>
 * <li>value: string literal repesenting the value</li>
 * <li>type (optional): fully qualified name of the type of the value. Defaults to
 * String.</li>
 * <li>identifying (optional): boolean to flag the job parameter as identifying or not.
 * Defaults to true</li>
 * </ul>
 *
 * For example, schedule.date={"value": "2022-12-12", "type":"java.time.LocalDate",
 * "identifying": "false"} will be converted to a non identifying job parameter of type
 * {@link java.time.LocalDate} with value "2022-12-12".
 *
 * The literal values are converted to the correct type by using the default Spring
 * conversion service, augmented if necessary by any custom converters. The conversion
 * service should be configured with a converter to and from string literals to job
 * parameter types.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 *
 */
public class JsonJobParametersConverter extends DefaultJobParametersConverter {

	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Create a new {@link JsonJobParametersConverter} with a default
	 * {@link ObjectMapper}.
	 */
	public JsonJobParametersConverter() {
		this(new ObjectMapper());
	}

	/**
	 * Create a new {@link JsonJobParametersConverter} with a custom {@link ObjectMapper}.
	 * @param objectMapper the object mapper to use
	 */
	public JsonJobParametersConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	protected String encode(JobParameter<?> jobParameter) {
		Class<?> parameterType = jobParameter.getType();
		Object parameterTypedValue = jobParameter.getValue();
		boolean parameterIdentifying = jobParameter.isIdentifying();
		String parameterStringValue = this.conversionService.convert(parameterTypedValue, String.class);
		try {
			return this.objectMapper.writeValueAsString(new JobParameterDefinition(parameterStringValue,
					parameterType.getName(), Boolean.toString(parameterIdentifying)));
		}
		catch (JsonProcessingException e) {
			throw new JobParametersConversionException("Unable to encode job parameter " + jobParameter, e);
		}
	}

	@Override
	protected JobParameter decode(String encodedJobParameter) {
		try {
			JobParameterDefinition jobParameterDefinition = this.objectMapper.readValue(encodedJobParameter,
					JobParameterDefinition.class);
			Class<?> parameterType = Class.forName(jobParameterDefinition.type());
			boolean parameterIdentifying = true;
			if (jobParameterDefinition.identifying() != null && !jobParameterDefinition.identifying().isEmpty()) {
				parameterIdentifying = Boolean.valueOf(jobParameterDefinition.identifying());
			}
			Object parameterTypedValue = this.conversionService.convert(jobParameterDefinition.value(), parameterType);
			return new JobParameter(parameterTypedValue, parameterType, parameterIdentifying);
		}
		catch (JsonProcessingException | ClassNotFoundException e) {
			throw new JobParametersConversionException("Unable to decode job parameter " + encodedJobParameter, e);
		}
	}

	public record JobParameterDefinition(String value, String type, String identifying) {
	}

}
