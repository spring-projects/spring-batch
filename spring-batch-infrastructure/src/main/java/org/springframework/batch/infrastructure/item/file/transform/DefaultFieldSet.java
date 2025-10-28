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

package org.springframework.batch.infrastructure.item.file.transform;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import java.util.Properties;

import org.springframework.util.Assert;

import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link FieldSet} using Java primitive and standard types and
 * utilities. Strings are trimmed before parsing by default, and so are plain String
 * values.
 *
 * @author Rob Harrop
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 * @author Stefano Cordio
 * @author Choi Wang Gyu
 */
@NullUnmarked // FIXME
public class DefaultFieldSet implements FieldSet {

	private final static String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";

	private DateFormat dateFormat;

	private NumberFormat numberFormat;

	private @Nullable String grouping;

	private @Nullable String decimal;

	/**
	 * The fields wrapped by this '<code>FieldSet</code>' instance.
	 */
	private final @Nullable String[] tokens;

	private @Nullable List<String> names;

	private @Nullable Map<String, Integer> nameIndexMap;

	/**
	 * Create a FieldSet with anonymous tokens.
	 * <p>
	 * They can only be retrieved by column number.
	 * @param tokens the token values
	 * @see FieldSet#readString(int)
	 */
	public DefaultFieldSet(@Nullable String @Nullable [] tokens) {
		this(tokens, null, null);
	}

	/**
	 * Create a FieldSet with anonymous tokens.
	 * <p>
	 * They can only be retrieved by column number.
	 * @param tokens the token values
	 * @param dateFormat the {@link DateFormat} to use
	 * @param numberFormat the {@link NumberFormat} to use
	 * @see FieldSet#readString(int)
	 * @since 5.2
	 */
	public DefaultFieldSet(@Nullable String @Nullable [] tokens, @Nullable DateFormat dateFormat,
			@Nullable NumberFormat numberFormat) {
		this.tokens = tokens != null ? tokens.clone() : new String[0];
		this.dateFormat = dateFormat != null ? dateFormat : getDefaultDateFormat();
		setNumberFormat(numberFormat != null ? numberFormat : getDefaultNumberFormat());
	}

	/**
	 * Create a FieldSet with named tokens.
	 * <p>
	 * The token values can then be retrieved either by name or by column number.
	 * @param tokens the token values
	 * @param names the names of the tokens
	 * @see FieldSet#readString(String)
	 */
	public DefaultFieldSet(@Nullable String[] tokens, String[] names) {
		this(tokens, names, getDefaultDateFormat(), getDefaultNumberFormat());
	}

	/**
	 * Create a FieldSet with named tokens.
	 * <p>
	 * The token values can then be retrieved either by name or by column number.
	 * @param tokens the token values
	 * @param names the names of the tokens
	 * @param dateFormat the {@link DateFormat} to use
	 * @param numberFormat the {@link NumberFormat} to use
	 * @see FieldSet#readString(String)
	 * @since 5.2
	 */
	public DefaultFieldSet(@Nullable String[] tokens, String[] names, @Nullable DateFormat dateFormat,
			@Nullable NumberFormat numberFormat) {
		Assert.notNull(tokens, "Tokens must not be null");
		Assert.notNull(names, "Names must not be null");
		if (tokens.length != names.length) {
			throw new IllegalArgumentException("Field names must be same length as values: names="
					+ Arrays.asList(names) + ", values=" + Arrays.asList(tokens));
		}
		this.tokens = tokens.clone();
		this.names = Arrays.asList(names);
		this.nameIndexMap = new HashMap<>(names.length);
		for (int i = 0; i < names.length; i++) {
			this.nameIndexMap.put(names[i], i);
		}
		this.dateFormat = dateFormat != null ? dateFormat : getDefaultDateFormat();
		setNumberFormat(numberFormat != null ? numberFormat : getDefaultNumberFormat());
	}

	private static DateFormat getDefaultDateFormat() {
		DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATE_PATTERN);
		dateFormat.setLenient(false);
		return dateFormat;
	}

	private static NumberFormat getDefaultNumberFormat() {
		return NumberFormat.getInstance(Locale.US);
	}

	/**
	 * The {@link DateFormat} to use for parsing dates.
	 * <p>
	 * If unset, the default pattern is ISO standard <code>yyyy-MM-dd</code>.
	 * @param dateFormat the {@link DateFormat} to use for date parsing
	 */
	public void setDateFormat(DateFormat dateFormat) {
		this.dateFormat = dateFormat;
	}

	/**
	 * The {@link NumberFormat} to use for parsing numbers.
	 * <p>
	 * If unset, {@link Locale#US} will be used ('.' as decimal place).
	 * @param numberFormat the {@link NumberFormat} to use for number parsing
	 */
	public final void setNumberFormat(NumberFormat numberFormat) {
		this.numberFormat = numberFormat;
		if (numberFormat instanceof DecimalFormat decimalFormat) {
			this.grouping = String.valueOf(decimalFormat.getDecimalFormatSymbols().getGroupingSeparator());
			this.decimal = String.valueOf(decimalFormat.getDecimalFormatSymbols().getDecimalSeparator());
		}
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
	public @Nullable String[] getValues() {
		return tokens.clone();
	}

	@Override
	public @Nullable String readString(int index) {
		return readAndTrim(index);
	}

	@Override
	public @Nullable String readString(String name) {
		return readString(indexOf(name));
	}

	@Override
	public @Nullable String readRawString(int index) {
		return tokens[index];
	}

	@Override
	public @Nullable String readRawString(String name) {
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
		return trueValue.equals(readAndTrim(index));
	}

	@Override
	public boolean readBoolean(String name, String trueValue) {
		return readBoolean(indexOf(name), trueValue);
	}

	@Override
	public char readChar(int index) {
		String value = Objects.requireNonNull(readAndTrim(index));
		Assert.isTrue(value.length() == 1, "Cannot convert field value '" + value + "' to char.");
		return value.charAt(0);
	}

	@Override
	public char readChar(String name) {
		return readChar(indexOf(name));
	}

	@Override
	public byte readByte(int index) {
		return Byte.parseByte(Objects.requireNonNull(readAndTrim(index)));
	}

	@Override
	public byte readByte(String name) {
		return readByte(indexOf(name));
	}

	@Override
	public short readShort(int index) {
		return Short.parseShort(Objects.requireNonNull(readAndTrim(index)));
	}

	@Override
	public short readShort(String name) {
		return readShort(indexOf(name));
	}

	@Override
	public int readInt(int index) {
		return parseNumber(Objects.requireNonNull(readAndTrim(index))).intValue();
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
		return parseNumber(Objects.requireNonNull(readAndTrim(index))).longValue();
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
		return parseNumber(Objects.requireNonNull(readAndTrim(index))).floatValue();
	}

	@Override
	public float readFloat(String name) {
		return readFloat(indexOf(name));
	}

	@Override
	public double readDouble(int index) {
		return parseNumber(Objects.requireNonNull(readAndTrim(index))).doubleValue();
	}

	@Override
	public double readDouble(String name) {
		return readDouble(indexOf(name));
	}

	@Override
	public @Nullable BigDecimal readBigDecimal(int index) {
		return readBigDecimal(index, null);
	}

	@Override
	public @Nullable BigDecimal readBigDecimal(String name) {
		return readBigDecimal(name, null);
	}

	@Override
	public @Nullable BigDecimal readBigDecimal(int index, @Nullable BigDecimal defaultValue) {
		String candidate = readAndTrim(index);

		if (!StringUtils.hasText(candidate)) {
			return defaultValue;
		}

		try {
			return new BigDecimal(removeSeparators(candidate));
		}
		catch (NumberFormatException e) {
			throw new NumberFormatException("Unparseable number: " + candidate);
		}
	}

	private String removeSeparators(String candidate) {
		return candidate.replace(grouping, "").replace(decimal, ".");
	}

	@Override
	public @Nullable BigDecimal readBigDecimal(String name, @Nullable BigDecimal defaultValue) {
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
		return parseDate(Objects.requireNonNull(readAndTrim(index)), dateFormat);
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
		return parseDate(Objects.requireNonNull(readAndTrim(index)), sdf);
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
	protected @Nullable String readAndTrim(int index) {
		String value = tokens[index];
		return value != null ? value.trim() : null;
	}

	/**
	 * Retrieve the index of where a specified column is located based on the {@code name}
	 * parameter.
	 * @param name the value to search in the {@link List} of names.
	 * @return the index in the {@link List} of names where the name was found.
	 * @throws IllegalArgumentException if a column with given name is not defined.
	 */
	protected int indexOf(String name) {
		if (nameIndexMap == null) {
			throw new IllegalArgumentException("Cannot access columns by name without meta data");
		}
		Integer index = nameIndexMap.get(name);
		if (index != null) {
			return index;
		}
		throw new IllegalArgumentException("Cannot access column [" + name + "] from " + names);
	}

	@Override
	public String toString() {
		if (names != null) {
			return getProperties().toString();
		}

		return Arrays.toString(tokens);
	}

	/**
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (object instanceof DefaultFieldSet fs) {
			return Arrays.equals(this.tokens, fs.tokens);
		}

		return false;
	}

	@Override
	public int hashCode() {
		// this algorithm was taken from java 1.5 jdk Arrays.hashCode(Object[])
		if (tokens.length == 0) {
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

	private Number parseNumber(String input) {
		try {
			return numberFormat.parse(input);
		}
		catch (ParseException e) {
			throw new NumberFormatException("Unparseable number: " + input);
		}
	}

	private Date parseDate(String input, DateFormat dateFormat) {
		try {
			return dateFormat.parse(input);
		}
		catch (ParseException e) {
			String pattern = dateFormat instanceof SimpleDateFormat sdf ? sdf.toPattern() : dateFormat.toString();
			throw new IllegalArgumentException(e.getMessage() + ", format: [" + pattern + "]");
		}
	}

}
