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

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link FieldSet} using Java using Java primitive
 * and standard types and utilities. Strings are trimmed before parsing by
 * default, and so are plain String values.
 * 
 * @author Rob Harrop
 * @author Dave Syer
 */
public class DefaultFieldSet implements FieldSet {

	private final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

	private DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
	{
		dateFormat.setLenient(false);
	}

	private NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

	private String grouping = ",";

	private String decimal = ".";

	/**
	 * The fields wrapped by this '<code>FieldSet</code>' instance.
	 */
	private String[] tokens;

	private List<String> names;

	/**
	 * The {@link NumberFormat} to use for parsing numbers. If unset the US
	 * locale will be used ('.' as decimal place).
	 * @param numberFormat the {@link NumberFormat} to use for number parsing
	 */
	public final void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		if (numberFormat instanceof DecimalFormat) {
			grouping = "" + ((DecimalFormat) numberFormat).getDecimalFormatSymbols().getGroupingSeparator();
			decimal = "" + ((DecimalFormat) numberFormat).getDecimalFormatSymbols().getDecimalSeparator();
		}
	}

	/**
	 * The {@link DateFormat} to use for parsing numbers. If unset the default
	 * pattern is ISO standard <code>yyyy/MM/dd</code>.
	 * @param dateFormat the {@link DateFormat} to use for date parsing
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * Create a FieldSet with anonymous tokens. They can only be retrieved by
	 * column number.
	 * @param tokens the token values
	 * @see FieldSet#readString(int)
	 */
	public DefaultFieldSet(String[] tokens) {
		this.tokens = tokens == null ? null : (String[]) tokens.clone();
		setNumberFormat(NumberFormat.getInstance(Locale.US));
	}

	/**
	 * Create a FieldSet with named tokens. The token values can then be
	 * retrieved either by name or by column number.
	 * @param tokens the token values
	 * @param names the names of the tokens
	 * @see FieldSet#readString(String)
	 */
	public DefaultFieldSet(String[] tokens, String[] names) {
		Assert.notNull(tokens);
		Assert.notNull(names);
		if (tokens.length != names.length) {
			throw new IllegalArgumentException("Field names must be same length as values: names="
					+ Arrays.asList(names) + ", values=" + Arrays.asList(tokens));
		}
		this.tokens = (String[]) tokens.clone();
		this.names = Arrays.asList(names);
		setNumberFormat(NumberFormat.getInstance(Locale.US));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#getNames()
	 */
	public String[] getNames() {
		if (names == null) {
			throw new IllegalStateException("Field names are not known");
		}
		return names.toArray(new String[names.size()]);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.FieldSet#hasNames()
	 */
	public boolean hasNames() {
		return names != null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#getValues()
	 */
	public String[] getValues() {
		return tokens.clone();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readString(int)
	 */
	public String readString(int index) {
		return readAndTrim(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readString(java
	 * .lang.String)
	 */
	public String readString(String name) {
		return readString(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readRawString(int)
	 */
	public String readRawString(int index) {
		return tokens[index];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readRawString(java
	 * .lang.String)
	 */
	public String readRawString(String name) {
		return readRawString(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBoolean(int)
	 */
	public boolean readBoolean(int index) {
		return readBoolean(index, "true");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBoolean(java
	 * .lang.String)
	 */
	public boolean readBoolean(String name) {
		return readBoolean(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBoolean(int,
	 * java.lang.String)
	 */
	public boolean readBoolean(int index, String trueValue) {
		Assert.notNull(trueValue, "'trueValue' cannot be null.");

		String value = readAndTrim(index);

		return trueValue.equals(value) ? true : false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBoolean(java
	 * .lang.String, java.lang.String)
	 */
	public boolean readBoolean(String name, String trueValue) {
		return readBoolean(indexOf(name), trueValue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readChar(int)
	 */
	public char readChar(int index) {
		String value = readAndTrim(index);

		Assert.isTrue(value.length() == 1, "Cannot convert field value '" + value + "' to char.");

		return value.charAt(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readChar(java.lang
	 * .String)
	 */
	public char readChar(String name) {
		return readChar(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readByte(int)
	 */
	public byte readByte(int index) {
		return Byte.parseByte(readAndTrim(index));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readByte(java.lang
	 * .String)
	 */
	public byte readByte(String name) {
		return readByte(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readShort(int)
	 */
	public short readShort(int index) {
		return Short.parseShort(readAndTrim(index));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readShort(java.
	 * lang.String)
	 */
	public short readShort(String name) {
		return readShort(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readInt(int)
	 */
	public int readInt(int index) {
		return parseNumber(readAndTrim(index)).intValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readInt(java.lang
	 * .String)
	 */
	public int readInt(String name) {
		return readInt(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readInt(int,
	 * int)
	 */
	public int readInt(int index, int defaultValue) {
		String value = readAndTrim(index);

		return StringUtils.hasLength(value) ? Integer.parseInt(value) : defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readInt(java.lang
	 * .String, int)
	 */
	public int readInt(String name, int defaultValue) {
		return readInt(indexOf(name), defaultValue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readLong(int)
	 */
	public long readLong(int index) {
		return parseNumber(readAndTrim(index)).longValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readLong(java.lang
	 * .String)
	 */
	public long readLong(String name) {
		return readLong(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readLong(int,
	 * long)
	 */
	public long readLong(int index, long defaultValue) {
		String value = readAndTrim(index);

		return StringUtils.hasLength(value) ? Long.parseLong(value) : defaultValue;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readLong(java.lang
	 * .String, long)
	 */
	public long readLong(String name, long defaultValue) {
		return readLong(indexOf(name), defaultValue);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readFloat(int)
	 */
	public float readFloat(int index) {
		return parseNumber(readAndTrim(index)).floatValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readFloat(java.
	 * lang.String)
	 */
	public float readFloat(String name) {
		return readFloat(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readDouble(int)
	 */
	public double readDouble(int index) {
		return (Double) parseNumber(readAndTrim(index)).doubleValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readDouble(java
	 * .lang.String)
	 */
	public double readDouble(String name) {
		return readDouble(indexOf(name));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBigDecimal(int)
	 */
	public BigDecimal readBigDecimal(int index) {
		return readBigDecimal(index, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBigDecimal(
	 * java.lang.String)
	 */
	public BigDecimal readBigDecimal(String name) {
		return readBigDecimal(name, null);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBigDecimal(int,
	 * java.math.BigDecimal)
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readBigDecimal(
	 * java.lang.String, java.math.BigDecimal)
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readDate(int)
	 */
	public Date readDate(int index) {
		return parseDate(readAndTrim(index), dateFormat);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.transform.FieldSet#readDate(int,
	 * java.util.Date)
	 */
	public Date readDate(int index, Date defaultValue) {
		try {
			return readDate(index);
		}
		catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readDate(java.lang
	 * .String)
	 */
	public Date readDate(String name) {
		try {
			return readDate(indexOf(name));
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.transform.FieldSet#readDate(int,
	 * java.util.Date)
	 */
	public Date readDate(String name, Date defaultValue) {
		try {
			return readDate(name);
		}
		catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readDate(int,
	 * java.lang.String)
	 */
	public Date readDate(int index, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		sdf.setLenient(false);
		return parseDate(readAndTrim(index), sdf);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readDate(int,
	 * java.lang.String)
	 */
	public Date readDate(int index, String pattern, Date defaultValue) {
		try {
			return readDate(index, pattern);
		}
		catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#readDate(java.lang
	 * .String, java.lang.String)
	 */
	public Date readDate(String name, String pattern) {
		try {
			return readDate(indexOf(name), pattern);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: [" + name + "]");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.item.file.mapping.IFieldSet#readDate(int,
	 * java.lang.String)
	 */
	public Date readDate(String name, String pattern, Date defaultValue) {
		try {
			return readDate(name, pattern);
		}
		catch (IllegalArgumentException e) {
			return defaultValue;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#getFieldCount()
	 */
	public int getFieldCount() {
		return tokens.length;
	}

	/**
	 * Read and trim the {@link String} value at '<code>index</code>'.
	 * 
	 * @returns null if the field value is <code>null</code>.
	 */
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
	 * Read and trim the {@link String} value from column with given '
	 * <code>name</code>.
	 * 
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
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

	public String toString() {
		if (names != null) {
			return getProperties().toString();
		}

		return tokens == null ? "" : Arrays.asList(tokens).toString();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (object instanceof DefaultFieldSet) {
			DefaultFieldSet fs = (DefaultFieldSet) object;

			if (this.tokens == null) {
				return fs.tokens == null;
			}
			else {
				return Arrays.equals(this.tokens, fs.tokens);
			}
		}

		return false;
	}

	public int hashCode() {
		// this algorithm was taken from java 1.5 jdk Arrays.hashCode(Object[])
		if (tokens == null) {
			return 0;
		}

		int result = 1;

		for (int i = 0; i < tokens.length; i++) {
			result = 31 * result + (tokens[i] == null ? 0 : tokens[i].hashCode());
		}

		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.batch.item.file.mapping.IFieldSet#getProperties()
	 */
	public Properties getProperties() {
		if (names == null) {
			throw new IllegalStateException("Cannot create properties without meta data");
		}
		Properties props = new Properties();
		for (int i = 0; i < tokens.length; i++) {
			String value = readAndTrim(i);
			if (value != null) {
				props.setProperty((String) names.get(i), value);
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
			if (dateFormat instanceof SimpleDateFormat) {
				pattern = ((SimpleDateFormat) dateFormat).toPattern();
			}
			else {
				pattern = dateFormat.toString();
			}
			throw new IllegalArgumentException(e.getMessage() + ", format: [" + pattern + "]");
		}
	}

}
