/*
 * Copyright 2022-2025 the original author or authors.
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

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;

/**
 * Converter for {@link JobParameters} instances based on Jackson 3 and that uses a JSON
 * naming convention for converting job parameters. The expected notation is the
 * following:
 * <p>
 * key='{"value": "parameterStringLiteralValue",
 * "type":"fully.qualified.name.of.the.parameter.Type", "identifying": "booleanValue"}'
 * <p>
 * where:
 *
 * <ul>
 * <li>value: string literal representing the value</li>
 * <li>type (optional): fully qualified name of the type of the value. Defaults to
 * String.</li>
 * <li>identifying (optional): boolean to flag the job parameter as identifying or not.
 * Defaults to true</li>
 * </ul>
 *
 * For example, schedule.date={"value": "2022-12-12", "type":"java.time.LocalDate",
 * "identifying": "false"} will be converted to a non identifying job parameter of type
 * {@link java.time.LocalDate} with value "2022-12-12".
 * <p>
 * The literal values are converted to the correct type by using the default Spring
 * conversion service, augmented if necessary by any custom converters. The conversion
 * service should be configured with a converter to and from string literals to job
 * parameter types.
 * <p>
 * By default, the Spring conversion service is augmented to support the conversion of the
 * following types:
 *
 * <ul>
 * <li>{@link java.util.Date}: in the
 * {@link java.time.format.DateTimeFormatter#ISO_INSTANT} format</li>
 * <li>{@link java.time.LocalDate}: in the
 * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE} format</li>
 * <li>{@link java.time.LocalTime}: in the
 * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_TIME} format</li>
 * <li>{@link java.time.LocalDateTime}: in the
 * {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE_TIME} format</li>
 * </ul>
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 *
 */
public class JsonJobParametersConverter extends DefaultJobParametersConverter {

	private final JsonMapper jsonMapper;

	/**
	 * Create a new {@link JsonJobParametersConverter} with a default {@link JsonMapper}.
	 */
	public JsonJobParametersConverter() {
		this(new JsonMapper());
	}

	/**
	 * Create a new {@link JsonJobParametersConverter} with a custom {@link JsonMapper}.
	 * @param jsonMapper the object mapper to use
	 */
	public JsonJobParametersConverter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	protected String encode(JobParameter<?> jobParameter) {
		Class<?> parameterType = jobParameter.type();
		String parameterName = jobParameter.name();
		Object parameterTypedValue = jobParameter.value();
		boolean parameterIdentifying = jobParameter.identifying();
		String parameterStringValue = this.conversionService.convert(parameterTypedValue, String.class);
		if (parameterStringValue == null) {
			throw new JobParametersConversionException(
					"Unable to encode job parameter of type " + parameterType + " with value " + parameterTypedValue);
		}
		try {
			return this.jsonMapper.writeValueAsString(new JobParameterDefinition(parameterName, parameterStringValue,
					parameterType.getName(), Boolean.toString(parameterIdentifying)));
		}
		catch (JacksonException e) {
			throw new JobParametersConversionException("Unable to encode job parameter " + jobParameter, e);
		}
	}

	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	@Override
	protected JobParameter decode(String parameterName, String encodedJobParameter) {
		try {
			JobParameterDefinition jobParameterDefinition = this.jsonMapper.readValue(encodedJobParameter,
					JobParameterDefinition.class);
			Class<?> parameterType = String.class;
			if (jobParameterDefinition.type() != null) {
				parameterType = Class.forName(jobParameterDefinition.type());
			}
			boolean parameterIdentifying = true;
			if (jobParameterDefinition.identifying() != null && !jobParameterDefinition.identifying().isEmpty()) {
				parameterIdentifying = Boolean.parseBoolean(jobParameterDefinition.identifying());
			}
			Object parameterTypedValue = this.conversionService.convert(jobParameterDefinition.value(), parameterType);
			return new JobParameter(parameterName, parameterTypedValue, parameterType, parameterIdentifying);
		}
		catch (JacksonException | ClassNotFoundException e) {
			throw new JobParametersConversionException("Unable to decode job parameter " + encodedJobParameter, e);
		}
	}

	public record JobParameterDefinition(String name, String value, String type, String identifying) {
	}

}
