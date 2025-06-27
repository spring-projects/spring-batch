/*
 * Copyright 2006-2025 the original author or authors.
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
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.batch.core.job.parameters.JobParameter;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Converter for {@link JobParameters} instances that uses a simple naming convention for
 * converting job parameters. The expected notation is the following:
 * <p>
 * key=value,type,identifying
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
 * For example, schedule.date=2022-12-12,java.time.LocalDate will be converted to an
 * identifying job parameter of type {@link java.time.LocalDate} with value "2022-12-12".
 * <p>
 * The literal values are converted to the target type by using the default Spring
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
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @deprecated since 6.0 with no replacement, scheduled for removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class DefaultJobParametersConverter implements JobParametersConverter {

	protected ConfigurableConversionService conversionService;

	public DefaultJobParametersConverter() {
		DefaultConversionService conversionService = new DefaultConversionService();
		conversionService.addConverter(new DateToStringConverter());
		conversionService.addConverter(new StringToDateConverter());
		conversionService.addConverter(new LocalDateToStringConverter());
		conversionService.addConverter(new StringToLocalDateConverter());
		conversionService.addConverter(new LocalTimeToStringConverter());
		conversionService.addConverter(new StringToLocalTimeConverter());
		conversionService.addConverter(new LocalDateTimeToStringConverter());
		conversionService.addConverter(new StringToLocalDateTimeConverter());
		this.conversionService = conversionService;
	}

	/**
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getJobParameters(java.util.Properties)
	 */
	@Override
	public JobParameters getJobParameters(@Nullable Properties properties) {
		if (properties == null || properties.isEmpty()) {
			return new JobParameters();
		}
		JobParametersBuilder jobParametersBuilder = new JobParametersBuilder();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			String parameterName = (String) entry.getKey();
			String encodedJobParameter = (String) entry.getValue();
			JobParameter<?> jobParameter = decode(encodedJobParameter);
			jobParametersBuilder.addJobParameter(parameterName, jobParameter);
		}
		return jobParametersBuilder.toJobParameters();
	}

	/**
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getProperties(JobParameters)
	 */
	@Override
	public Properties getProperties(@Nullable JobParameters jobParameters) {
		if (jobParameters == null || jobParameters.isEmpty()) {
			return new Properties();
		}
		Map<String, JobParameter<?>> parameters = jobParameters.getParameters();
		Properties properties = new Properties();
		for (Entry<String, JobParameter<?>> entry : parameters.entrySet()) {
			String parameterName = entry.getKey();
			JobParameter<?> jobParameter = entry.getValue();
			properties.setProperty(parameterName, encode(jobParameter));
		}
		return properties;
	}

	/**
	 * Set the conversion service to use.
	 * @param conversionService the conversion service to use. Must not be {@code null}.
	 * @since 5.0
	 */
	public void setConversionService(@NonNull ConfigurableConversionService conversionService) {
		Assert.notNull(conversionService, "The conversionService must not be null");
		this.conversionService = conversionService;
	}

	/**
	 * Encode a job parameter to a string.
	 * @param jobParameter the parameter to encode
	 * @return the encoded job parameter
	 */
	protected String encode(JobParameter<?> jobParameter) {
		Class<?> parameterType = jobParameter.getType();
		boolean parameterIdentifying = jobParameter.isIdentifying();
		Object parameterTypedValue = jobParameter.getValue();
		String parameterStringValue = this.conversionService.convert(parameterTypedValue, String.class);
		return String.join(",", parameterStringValue, parameterType.getName(), Boolean.toString(parameterIdentifying));
	}

	/**
	 * Decode a job parameter from a string.
	 * @param encodedJobParameter the encoded job parameter
	 * @return the decoded job parameter
	 */
	@SuppressWarnings(value = { "unchecked", "rawtypes" })
	protected JobParameter<?> decode(String encodedJobParameter) {
		String parameterStringValue = parseValue(encodedJobParameter);
		Class<?> parameterType = parseType(encodedJobParameter);
		boolean parameterIdentifying = parseIdentifying(encodedJobParameter);
		try {
			Object typedValue = this.conversionService.convert(parameterStringValue, parameterType);
			return new JobParameter(typedValue, parameterType, parameterIdentifying);
		}
		catch (Exception e) {
			throw new JobParametersConversionException(
					"Unable to convert job parameter " + parameterStringValue + " to type " + parameterType, e);
		}
	}

	private String parseValue(String encodedJobParameter) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(encodedJobParameter);
		if (tokens.length == 0) {
			return "";
		}
		return tokens[0];
	}

	private Class<?> parseType(String encodedJobParameter) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(encodedJobParameter);
		if (tokens.length <= 1) {
			return String.class;
		}
		try {
			Class<?> type = Class.forName(tokens[1]);
			return type;
		}
		catch (ClassNotFoundException e) {
			throw new JobParametersConversionException("Unable to parse job parameter " + encodedJobParameter, e);
		}
	}

	private boolean parseIdentifying(String encodedJobParameter) {
		String[] tokens = StringUtils.commaDelimitedListToStringArray(encodedJobParameter);
		if (tokens.length <= 2) {
			return true;
		}
		return Boolean.parseBoolean(tokens[2]);
	}

}
