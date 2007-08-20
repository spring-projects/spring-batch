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

package org.springframework.batch.io.file;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Rob Harrop
 * @author Dave Syer
 */
public final class FieldSet {

	private final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

	/**
	 * The fields wrapped by this '<code>FieldSet</code>' instance.
	 */
	private String[] tokens;

	private List names;

	public FieldSet(String[] tokens) {
		this.tokens = tokens;
	}

	public FieldSet(String[] tokens, String[] names) {
		if (tokens.length != names.length) {
			throw new IllegalArgumentException(
					"Field names must be same length as values: names="
							+ Arrays.asList(names) + ", values="
							+ Arrays.asList(tokens));
		}
		this.tokens = tokens;
		this.names = Arrays.asList(names);
	}

	/**
	 * Read the {@link String} value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public String readString(int index) {
		return readAndTrim(index);
	}

	/**
	 * Read the {@link String} value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 */
	public String readString(String name) {
		return readString(indexOf(name));
	}

	/**
	 * Read the '<code>boolean</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public boolean readBoolean(int index) {
		return readBoolean(index, "true");
	}

	/**
	 * Read the '<code>boolean</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public boolean readBoolean(String name) {
		return readBoolean(indexOf(name));
	}

	/**
	 * Read the '<code>boolean</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @param trueValue
	 *            the value that signifies {@link Boolean#TRUE true};
	 *            case-sensitive.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds, or if the supplied
	 *             <code>trueValue</code> is <code>null</code>.
	 */
	public boolean readBoolean(int index, String trueValue) {
		Assert.notNull(trueValue, "'trueValue' cannot be null.");

		String value = readAndTrim(index);

		return trueValue.equals(value) ? true : false;
	}

	/**
	 * Read the '<code>boolean</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 * @param trueValue
	 *            the value that signifies {@link Boolean#TRUE true};
	 *            case-sensitive.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined, or if the
	 *             supplied <code>trueValue</code> is <code>null</code>.
	 */
	public boolean readBoolean(String name, String trueValue) {
		return readBoolean(indexOf(name), trueValue);
	}

	/**
	 * Read the '<code>char</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public char readChar(int index) {
		String value = readAndTrim(index);

		Assert.isTrue(value.length() == 1, "Cannot convert field value '"
				+ value + "' to char.");

		return value.charAt(0);
	}

	/**
	 * Read the '<code>char</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public char readChar(String name) {
		return readChar(indexOf(name));
	}

	/**
	 * Read the '<code>byte</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public byte readByte(int index) {
		return Byte.parseByte(readAndTrim(index));
	}

	/**
	 * Read the '<code>byte</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 */
	public byte readByte(String name) {
		return readByte(indexOf(name));
	}

	/**
	 * Read the '<code>short</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public short readShort(int index) {
		return Short.parseShort(readAndTrim(index));
	}

	/**
	 * Read the '<code>short</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public short readShort(String name) {
		return readShort(indexOf(name));
	}

	/**
	 * Read the '<code>int</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public int readInt(int index) {
		return Integer.parseInt(readAndTrim(index));
	}

	/**
	 * Read the '<code>int</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public int readInt(String name) {
		return readInt(indexOf(name));
	}

	/**
	 * Read the '<code>int</code>' value at index '<code>index</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param index
	 *            the field index..
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public int readInt(int index, int defaultValue) {
		String value = readAndTrim(index);

		return StringUtils.hasLength(value) ? Integer.parseInt(value)
				: defaultValue;
	}

	/**
	 * Read the '<code>int</code>' value from column with given '<code>name</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public int readInt(String name, int defaultValue) {
		return readInt(indexOf(name), defaultValue);
	}

	/**
	 * Read the '<code>long</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public long readLong(int index) {
		return Long.parseLong(readAndTrim(index));
	}

	/**
	 * Read the '<code>long</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public long readLong(String name) {
		return readLong(indexOf(name));
	}

	/**
	 * Read the '<code>long</code>' value at index '<code>index</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param index
	 *            the field index..
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public long readLong(int index, long defaultValue) {
		String value = readAndTrim(index);

		return StringUtils.hasLength(value) ? Long.parseLong(value)
				: defaultValue;
	}

	/**
	 * Read the '<code>long</code>' value from column with given '<code>name</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public long readLong(String name, long defaultValue) {
		return readLong(indexOf(name), defaultValue);
	}

	/**
	 * Read the '<code>float</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public float readFloat(int index) {
		return Float.parseFloat(readAndTrim(index));
	}

	/**
	 * Read the '<code>float</code>' value from column with given '<code>name</code>.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public float readFloat(String name) {
		return readFloat(indexOf(name));
	}

	/**
	 * Read the '<code>double</code>' value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public double readDouble(int index) {
		return Double.parseDouble(readAndTrim(index));
	}

	/**
	 * Read the '<code>double</code>' value from column with given '<code>name</code>.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public double readDouble(String name) {
		return readDouble(indexOf(name));
	}

	/**
	 * Read the {@link java.math.BigDecimal} value at index '<code>index</code>'.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public BigDecimal readBigDecimal(int index) {
		return readBigDecimal(index, null);
	}

	/**
	 * Read the {@link java.math.BigDecimal} value from column with given '<code>name</code>.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public BigDecimal readBigDecimal(String name) {
		return readBigDecimal(name, null);
	}

	/**
	 * Read the {@link BigDecimal} value at index '<code>index</code>',
	 * returning the supplied <code>defaultValue</code> if the trimmed string
	 * value at index '<code>index</code>' is blank.
	 * 
	 * @param index
	 *            the field index.
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 */
	public BigDecimal readBigDecimal(int index, BigDecimal defaultValue) {
		String candidate = readAndTrim(index);

		try {
			return (StringUtils.hasText(candidate)) ? new BigDecimal(candidate)
					: defaultValue;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Unparseable number: "
					+ candidate);
		}
	}

	/**
	 * Read the {@link BigDecimal} value from column with given '<code>name</code>,
	 * returning the supplied <code>defaultValue</code> if the trimmed string
	 * value at index '<code>index</code>' is blank.
	 * 
	 * @param name
	 *            the field name.
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	public BigDecimal readBigDecimal(String name, BigDecimal defaultValue) {
		try {
			return readBigDecimal(indexOf(name), defaultValue);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: ["
					+ name + "]");
		}
	}

	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index
	 *            the field index.
	 * @param pattern
	 *            the pattern describing the date and time format
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 * @see #DEFAULT_DATE_PATTERN
	 */
	public Date readDate(int index) {
		return readDate(index, DEFAULT_DATE_PATTERN);
	}

	/**
	 * Read the <code>java.sql.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name
	 *            the field name.
	 * @param pattern
	 *            the pattern describing the date and time format
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 * @see #DEFAULT_DATE_PATTERN
	 */
	public Date readDate(String name) {
		return readDate(name, DEFAULT_DATE_PATTERN);
	}

	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index
	 *            the field index.
	 * @param pattern
	 *            the pattern describing the date and time format
	 * @throws IndexOutOfBoundsException
	 *             if the index is out of bounds.
	 * @throws IllegalArgumentException
	 *             if the date cannot be parsed.
	 * 
	 */
	public Date readDate(int index, String pattern) {
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		Date date;
		String value = readAndTrim(index);
		try {
			date = sdf.parse(value);
		} catch (ParseException e) {
			throw new IllegalArgumentException(e.getMessage() + ", pattern: ["
					+ pattern + "]");
		}
		return date;
	}

	/**
	 * Read the <code>java.sql.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name
	 *            the field name.
	 * @param pattern
	 *            the pattern describing the date and time format
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined or if the
	 *             specified field cannot be parsed
	 * 
	 */
	public Date readDate(String name, String pattern) {
		try {
			return readDate(indexOf(name), pattern);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(e.getMessage() + ", name: ["
					+ name + "]");
		}
	}

	/**
	 * Return the number of fields in this '<code>FieldSet</code>'.
	 */
	public int getFieldCount() {
		return tokens.length;
	}

	/**
	 * Read and trim the {@link String} value at '<code>index</code>'.
	 * 
	 * @throws NullPointerException
	 *             if the field value is <code>null</code>.
	 */
	private String readAndTrim(int index) {
		String value = tokens[index];

		if (value != null) {
			return value.trim();
		} else {
			return value;
		}
	}

	/**
	 * Read and trim the {@link String} value from column with given '<code>name</code>.
	 * 
	 * @throws IllegalArgumentException
	 *             if a column with given name is not defined.
	 */
	private int indexOf(String name) {
		if (names == null) {
			throw new IllegalArgumentException(
					"Cannot access columns by name without meta data");
		}
		int index = names.indexOf(name);
		if (index >= 0) {
			return index;
		}
		throw new IllegalArgumentException("Cannot access column [" + name
				+ "] from " + names);
	}

	public String toString() {
		if (names != null) {
			return getProperties().toString();
		}
		// TODO return "" instead of null?
		return tokens == null ? null : Arrays.asList(tokens).toString();
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object object) {
		if (object instanceof FieldSet) {
			FieldSet fs = (FieldSet) object;

			if (this.tokens == null) {
				return fs.tokens == null;
			} else {
				return Arrays.equals(this.tokens, fs.tokens);
			}
		}

		return false;
	}

	public int hashCode() {
		return (tokens == null) ? 0 : tokens.hashCode();
	}

	/**
	 * Construct name-value pairs from the field names and string values.
	 * 
	 * @return some properties representing the fle set.
	 * 
	 * @throws IllegalStateException
	 *             if the field name meta data is not available.
	 */
	public Properties getProperties() {
		if (names == null) {
			throw new IllegalStateException(
					"Cannot create properties without meta data");
		}
		Properties props = new Properties();
		for (int i = 0; i < tokens.length; i++) {
			props.setProperty((String) names.get(i), tokens[i]);
		}
		return props;
	}

}
