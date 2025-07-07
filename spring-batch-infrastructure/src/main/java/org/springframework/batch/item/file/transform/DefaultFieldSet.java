/*
 * Copyright 2006-2024 the original author or authors.
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

package org.springframework.batch.item.file.transform;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link FieldSet} using Java primitive and standard types and
 * utilities. Strings are trimmed before parsing by default, and so are plain String
 * values.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 */
public class DefaultFieldSet implements FieldSet {

	private final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

	private DateFormat dateFormat;

	private NumberFormat numberFormat;

	private String grouping;

	private String decimal;

	/**
	 * The fields wrapped by this '<code>FieldSet</code>' instance.
	 */
	private final String[] tokens;

	private List<String> names;

	/**
	 * The {@link NumberFormat} to use for parsing numbers. If unset the {@link Locale#US}
	 * will be used ('.' as decimal place).
	 * @param numberFormat the {@link NumberFormat} to use for number parsing
	 */
	public final void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		if (numberFormat instanceof DecimalFormat decimalFormat) {
			grouping = String.valueOf(decimalFormat.getDecimalFormatSymbols().getGroupingSeparator());
			decimal = String.valueOf(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator());
		}
	}

	private static NumberFormat getDefaultNumberFormat() {
		return NumberFormat.getInstance(Locale.US);
	}

	/**
	 * The {@link DateFormat} to use for parsing dates. If unset the default pattern is
	 * ISO standard <code>yyyy-MM-dd</code>.
	 * @param dateFormat the {@link DateFormat} to use for date parsing
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	private static DateFormat getDefaultDateFormat() {
		DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
		dateFormat.setLenient(false);
		return dateFormat;
	}

	/**
	 * Create a FieldSet with anonymous tokens. They can only be retrieved by column
	 * number.
	 * @param tokens the token values
	 * @param dateFormat the {@link DateFormat} to use
	 * @param numberFormat the {@link NumberFormat} to use
	 * @see FieldSet#readString(int)
	 * @since 5.2
	 */
	public DefaultFieldSet(String[] tokens, @Nullable DateFormat dateFormat, @Nullable NumberFormat numberFormat) {
		this.tokens = tokens == null ? null : tokens.clone();
		setDateFormat(dateFormat == null ? getDefaultDateFormat() : dateFormat);
		setNumberFormat(numberFormat == null ? getDefaultNumberFormat() : numberFormat);
	}

	/**
	 * Create a FieldSet with anonymous tokens. They can only be retrieved by column
	 * number.
	 * @param tokens the token values
	 * @see FieldSet#readString(int)
	 */
	public DefaultFieldSet(String[] tokens) {
		this(tokens, null, null);
	}

	/**
	 * Create a FieldSet with named tokens. The token values can then be retrieved either
	 * by name or by column number.
	 * @param tokens the token values
	 * @param names the names of the tokens
	 * @param dateFormat the {@link DateFormat} to use
	 * @param numberFormat the {@link NumberFormat} to use
	 * @see FieldSet#readString(String)
	 * @since 5.2
	 */
	public DefaultFieldSet(String[] tokens, String[] names, @Nullable DateFormat dateFormat,
			@Nullable NumberFormat numberFormat) {
		Assert.notNull(tokens, "Tokens must not be null");
		Assert.notNull(names, "Names must not be null");
		if (tokens.length != names.length) {
			throw new IllegalArgumentException("Field names must be same length as values: names="
					+ Arrays.asList(names) + ", values=" + Arrays.asList(tokens));
		}
		this.tokens = tokens.clone();
		this.names = Arrays.asList(names);
		setDateFormat(dateFormat == null ? getDefaultDateFormat() : dateFormat);
		setNumberFormat(numberFormat == null ? getDefaultNumberFormat() : numberFormat);
	}

	/**
	 * Create a FieldSet with named tokens. The token values can then be retrieved either
	 * by name or by column number.
	 * @param tokens the token values
	 * @param names the names of the tokens
	 * @see FieldSet#readString(String)
	 */
	public DefaultFieldSet(String[] tokens, String[] names) {
		this(tokens, names, null, null);
	}

	@Override
	public String[] getNames() {
		if (names == null) {
			throw new IllegalStateException("Field names are not known");
		}
		return names.toArray(new String[0]);
	}

	@Override
	public boolean hasNames() {
		return names != null;
	}

	@Override
	public String[] getValues() {
		return tokens.clone();
	}

	@Override
	public String readString(int index) {
		return readAndTrim(index);
	}

	@Override
	public String readString(String name) {
		return readString(indexOf(name));
	}

	@Override
	public String readRawString(int index) {
		return tokens[index];
	}

	@Override
	public String readRawString(String name) {
		return readRawString(indexOf(name));
	}

	@Override
	public boolean readBoolean(int index) {
		return readBoolean(index, "true");
	}

	@Override
	public boolean readBoolean(String name) {
		return readBoolean(indexOf(name));
	}

	@Override
	public boolean readBoolean(int index, String trueValue) {
		Assert.notNull(trueValue, "'trueValue' cannot be null.");

		String value = readAndTrim(index);

		return trueValue.equals(value);
	}

	@Override
	public boolean readBoolean(String name, String trueValue) {
		return readBoolean(indexOf(name), trueValue);
	}

	@Override
	public char readChar(int index) {
		String value = readAndTrim(index);

		Assert.isTrue(value.length() == 1, "Cannot convert field value '" + value + "' to char.");

		return value.charAt(0);
	}

	@Override
	public char readChar(String name) {
		return readChar(indexOf(name));
	}

	@Override
	public byte readByte(int index) {
		return Byte.parseByte(readAndTrim(index));
	}

	@Override
	public byte readByte(String name) {
		return readByte(indexOf(name));
	}

	@Override
	public short readShort(int index) {
		return Short.parseShort(readAndTrim(index));
	}

	@Override
	public short readShort(String name) {
		return readShort(indexOf(name));
	}

	@Override
	public int readInt(int index) {
		return parseNumber(readAndTrim(index)).intValue();
	}

	@Override
	public int readInt(String name) {
		return readInt(indexOf(name));
	}

	@Override
	public int readInt(int index, int defaultValue) {
		String value = readAndTrim(index);

		return StringUtils.hasLength(value) ? Integer.parseInt(value) : defaultValue;
	}

	@Override
	public int readInt(String name, int defaultValue) {
		return readInt(indexOf(name), defaultValue);
	}

	@Override
	public long readLong(int index) {
		return parseNumber(readAndTrim(index)).longValue();
	}

	@Override
	public long readLong(String name) {
		return readLong(indexOf(name));
	}

	@Override
	public long readLong(int index, long defaultValue) {
		String value = readAndTrim(index);

		return StringUtils.hasLength(value) ? Long.parseLong(value) : defaultValue;
	}

	@Override
	public long readLong(String name, long defaultValue) {
		return readLong(indexOf(name), defaultValue);
	}

	@Override
	public float readFloat(int index) {
		return parseNumber(readAndTrim(index)).floatValue();
	}

	@Override
	public float readFloat(String name) {
		return readFloat(indexOf(name));
	}

	@Override
	public double readDouble(int index) {
		return parseNumber(readAndTrim(index)).doubleValue();
	}

	@Override
	public double readDouble(String name) {
		return readDouble(indexOf(name));
	}

	@Override
	public BigDecimal readBigDecimal(int index) {
		return readBigDecimal(index, null);
	}

	@Override
	public BigDecimal readBigDecimal(String name) {
		return readBigDecimal(name, null);
	}

	@Override
	public BigDecimal readBigDecimal(int index, BigDecimal defaultValue) {
		String candidate = readAndTrim(index);

		if (!StringUtils.hasText(candidate)) {
			return defaultValue;
		}

		try {
			String result = removeSeparators(candidate);
			return new BigDecimal(result);
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException("Unparseable number: " + candidate);
		}
	}

	private String removeSeparators(String candidate) {
		return candidate.replace(grouping, "").replace(decimal, ".");
	}

	@Override
	public BigDecimal readBigDecimal(String name, BigDecimal defaultValue) {
		try {
			return readBigDecimal(indexOf(name), defaultValue);
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException(e.getMessage() + ", name: [" + name + "]");
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	@Override
	public Date readDate(int index) {
		return parseDate(readAndTrim(index), dateFormat);
	}

	@Override
	public Date readDate(int index, Date defaultValue) {
		String candidate = readAndTrim(index);
		return StringUtils.hasText(candidate) ? parseDate(candidate, dateFormat) : defaultValue;
	}

	@Override
	public Date readDate(String name) {
		try {
			return readDate(indexOf(name));
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	@Override
	public Date readDate(String name, Date defaultValue) {
		try {
			return readDate(indexOf(name), defaultValue);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	@Override
	public Date readDate(int index, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		sdf.setLenient(false);
		return parseDate(readAndTrim(index), sdf);
	}

	@Override
	public Date readDate(int index, String pattern, Date defaultValue) {
		String candidate = readAndTrim(index);
		return StringUtils.hasText(candidate) ? readDate(index, pattern) : defaultValue;
	}

	@Override
	public Date readDate(String name, String pattern) {
		try {
			return readDate(indexOf(name), pattern);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	@Override
	public Date readDate(String name, String pattern, Date defaultValue) {
		try {
			return readDate(indexOf(name), pattern, defaultValue);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	@Override
	public int getFieldCount() {
		return tokens.length;
	}

	/**
	 * Read and trim the {@link String} value at '<code>index</code>'.
	 * @param index the offset in the token array to obtain the value to be trimmed.
	 * @return null if the field value is <code>null</code>.
	 */
	@Nullable
	protected String readAndTrim(int index) {
		String value = tokens[index];

		if (value != null) {
			return value.trim();
		}
		else {
			return null;
		}
	}

	/**
	 * Retrieve the index of where a specified column is located based on the {@code name}
	 * parameter.
	 * @param name the value to search in the {@link List} of names.
	 * @return the index in the {@link List} of names where the name was found.
	 * @throws IllegalArgumentException if a column with given name is not defined.
	 */
	protected int indexOf(String name) {
		if (names == null) {
			throw new IllegalArgumentException("Cannot access columns by name without meta data");
		}
		int index = names.indexOf(name);
		if (index >= 0) {
			return index;
		}
		throw new IllegalArgumentException("Cannot access column [" + name + "] from " + names);
	}

	@Override
	public String toString() {
		if (names != null) {
			return getProperties().toString();
		}

		return tokens == null ? "" : Arrays.asList(tokens).toString();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof DefaultFieldSet fs) {

			if (this.tokens == null) {
				return fs.tokens == null;
			}
			else {
				return Arrays.equals(this.tokens, fs.tokens);
			}
		}

		return false;
	}

	@Override
	public int hashCode() {
		// this algorithm was taken from java 1.5 jdk Arrays.hashCode(Object[])
		if (tokens == null) {
			return 0;
		}

		int result = 1;

		for (String token : tokens) {
			result = 31 * result + (token == null ? 0 : token.hashCode());
		}

		return result;
	}

	@Override
	public Properties getProperties() {
		if (names == null) {
			throw new IllegalStateException("Cannot create properties without meta data");
		}
		Properties props = new Properties();
		for (int i = 0; i < tokens.length; i++) {
			String value = readAndTrim(i);
			if (value != null) {
				props.setProperty(names.get(i), value);
			}
		}
		return props;
	}

	private Number parseNumber(String candidate) {
		try {
			return numberFormat.parse(candidate);
		}
		catch (ParseException e) {
			throw new NumberFormatException("Unparseable number: " + candidate);
		}
	}

	private Date parseDate(String readAndTrim, DateFormat dateFormat) {
		try {
			return dateFormat.parse(readAndTrim);
		}
		catch (ParseException e) {
			String pattern;
			if (dateFormat instanceof SimpleDateFormat simpleDateFormat) {
				pattern = simpleDateFormat.toPattern();
			}
			else {
				pattern = dateFormat.toString();
			}
			throw new IllegalArgumentException(e.getMessage() + ", format: [" + pattern + "]");
		}
	}

}
