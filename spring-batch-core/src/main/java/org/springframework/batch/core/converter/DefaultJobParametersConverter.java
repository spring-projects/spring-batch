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
package org.springframework.batch.core.converter;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameter.ParameterType;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.util.StringUtils;

/**
 * Converter for {@link JobParameters} instances using a simple naming
 * convention for property keys. Key names ending with "(&lt;type&gt;)" where
 * type is one of string, date, long are converted to the corresponding type.
 * The default type is string. E.g.
 * 
 * <pre>
 * schedule.date(date)=2007/12/11
 * department.id(long)=2345
 * </pre>
 * 
 * The literal values are converted to the correct type using the default Spring
 * strategies, augmented if necessary by the custom editors provided.
 * 
 * <br/>
 * 
 * If you need to be able to parse and format local-specific dates and numbers,
 * you can inject formatters ({@link #setDateFormat(DateFormat)} and
 * {@link #setNumberFormat(NumberFormat)}).
 * 
 * @author Dave Syer
 * 
 */
public class DefaultJobParametersConverter implements JobParametersConverter {

	public static final String DATE_TYPE = "(date)";

	public static final String STRING_TYPE = "(string)";

	public static final String LONG_TYPE = "(long)";

	private static final String DOUBLE_TYPE = "(double)";

	private static NumberFormat DEFAULT_NUMBER_FORMAT = NumberFormat.getInstance(Locale.US);

	private DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");

	private NumberFormat numberFormat = DEFAULT_NUMBER_FORMAT;

	private final NumberFormat longNumberFormat = new DecimalFormat("#");

	/**
	 * Check for suffix on keys and use those to decide how to convert the
	 * value.
	 * 
	 * @throws IllegalArgumentException if a number or date is passed in that
	 * cannot be parsed, or cast to the correct type.
	 * 
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getJobParameters(java.util.Properties)
	 */
	public JobParameters getJobParameters(Properties props) {

		if (props == null || props.isEmpty()) {
			return new JobParameters();
		}

		JobParametersBuilder propertiesBuilder = new JobParametersBuilder();

		for (Iterator<Entry<Object, Object>> it = props.entrySet().iterator(); it.hasNext();) {
			Entry<Object, Object> entry = it.next();
			String key = (String) entry.getKey();
			String value = (String) entry.getValue();
			if (key.endsWith(DATE_TYPE)) {
				Date date;
				try {
					date = dateFormat.parse(value);
				}
				catch (ParseException ex) {
					String suffix = (dateFormat instanceof SimpleDateFormat) ? ", use "
							+ ((SimpleDateFormat) dateFormat).toPattern() : "";
					throw new IllegalArgumentException("Date format is invalid: [" + value + "]" + suffix);
				}
				propertiesBuilder.addDate(StringUtils.replace(key, DATE_TYPE, ""), date);
			}
			else if (key.endsWith(LONG_TYPE)) {
				Long result;
				try {
					result = (Long) parseNumber(value);
				}
				catch (ClassCastException ex) {
					throw new IllegalArgumentException("Number format is invalid for long value: [" + value
							+ "], use a format with no decimal places");
				}
				propertiesBuilder.addLong(StringUtils.replace(key, LONG_TYPE, ""), result);
			}
			else if (key.endsWith(DOUBLE_TYPE)) {
				Double result = parseNumber(value).doubleValue();
				propertiesBuilder.addDouble(StringUtils.replace(key, DOUBLE_TYPE, ""), result);
			}
			else if (StringUtils.endsWithIgnoreCase(key, STRING_TYPE)) {
				propertiesBuilder.addString(StringUtils.replace(key, STRING_TYPE, ""), value);
			}
			else {
				propertiesBuilder.addString(key, value);
			}
		}

		return propertiesBuilder.toJobParameters();
	}

	/**
	 * Delegate to {@link NumberFormat} to parse the value
	 */
	private Number parseNumber(String value) {
		try {
			return numberFormat.parse(value);
		}
		catch (ParseException ex) {
			String suffix = (numberFormat instanceof DecimalFormat) ? ", use "
					+ ((DecimalFormat) numberFormat).toPattern() : "";
			throw new IllegalArgumentException("Number format is invalid: [" + value + "], use " + suffix);
		}
	}

	/**
	 * Use the same suffixes to create properties (omitting the string suffix
	 * because it is the default).
	 * 
	 * @see org.springframework.batch.core.converter.JobParametersConverter#getProperties(org.springframework.batch.core.JobParameters)
	 */
	public Properties getProperties(JobParameters params) {

		if (params == null || params.isEmpty()) {
			return new Properties();
		}

		Map<String, JobParameter> parameters = params.getParameters();
		Properties result = new Properties();
		for (Entry<String, JobParameter> entry : parameters.entrySet()) {

			String key = entry.getKey();
			JobParameter jobParameter = entry.getValue();
			Object value = jobParameter.getValue();
			if (value != null) {
				if (jobParameter.getType() == ParameterType.DATE) {
					result.setProperty(key + DATE_TYPE, dateFormat.format(value));
				}
				else if (jobParameter.getType() == ParameterType.LONG) {
					result.setProperty(key + LONG_TYPE, longNumberFormat.format(value));
				}
				else if (jobParameter.getType() == ParameterType.DOUBLE) {
					result.setProperty(key + DOUBLE_TYPE, decimalFormat((Double)value));
				}
				else {
					result.setProperty(key, "" + value);
				}
			}
		}
		return result;
	}

	/**
	 * @param value a decimal value
	 * @return a best guess at the desired format
	 */
	private String decimalFormat(double value) {
		if (numberFormat != DEFAULT_NUMBER_FORMAT) {
			return numberFormat.format(value);
		}
		return Double.toString(value);
	}

	/**
	 * Public setter for injecting a date format.
	 * 
	 * @param dateFormat a {@link DateFormat}, defaults to "yyyy/MM/dd"
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Public setter for the {@link NumberFormat}. Used to parse longs and
	 * doubles, so must not contain decimal place (e.g. use "#" or "#,###").
	 * 
	 * @param numberFormat the {@link NumberFormat} to set
	 */
	public void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
	}
}
