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
import java.sql.ResultSet;
import java.util.Date;
import java.util.Properties;

/**
 * Interface used by flat file input sources to encapsulate concerns of
 * converting an array of Strings to Java native types. A bit like the role
 * played by {@link ResultSet} in JDBC, clients will know the name or position
 * of strongly typed fields that they want to extract.
 * 
 * @author Dave Syer
 * 
 */
public interface FieldSet {

	/**
	 * Accessor for the names of the fields.
	 * 
	 * @return the names
	 * 
	 * @throws IllegalStateException if the names are not defined
	 */
	String[] getNames();

	/**
	 * Check if there are names defined for the fields.
	 * 
	 * @return true if there are names for the fields
	 */
	boolean hasNames();

	/**
	 * @return fields wrapped by this '<code>FieldSet</code>' instance as
	 * String values.
	 */
	String[] getValues();

	/**
	 * Read the {@link String} value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	String readString(int index);

	/**
	 * Read the {@link String} value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	String readString(String name);

	/**
	 * Read the {@link String} value at index '<code>index</code>' including
	 * trailing whitespace (don't trim).
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	String readRawString(int index);

	/**
	 * Read the {@link String} value from column with given '<code>name</code>'
	 * including trailing whitespace (don't trim).
	 * 
	 * @param name the field name.
	 */
	String readRawString(String name);

	/**
	 * Read the '<code>boolean</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	boolean readBoolean(int index);

	/**
	 * Read the '<code>boolean</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	boolean readBoolean(String name);

	/**
	 * Read the '<code>boolean</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @param trueValue the value that signifies {@link Boolean#TRUE true};
	 * case-sensitive.
	 * @throws IndexOutOfBoundsException if the index is out of bounds, or if
	 * the supplied <code>trueValue</code> is <code>null</code>.
	 */
	boolean readBoolean(int index, String trueValue);

	/**
	 * Read the '<code>boolean</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @param trueValue the value that signifies {@link Boolean#TRUE true};
	 * case-sensitive.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined, or if the supplied <code>trueValue</code> is <code>null</code>.
	 */
	boolean readBoolean(String name, String trueValue);

	/**
	 * Read the '<code>char</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	char readChar(int index);

	/**
	 * Read the '<code>char</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	char readChar(String name);

	/**
	 * Read the '<code>byte</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	byte readByte(int index);

	/**
	 * Read the '<code>byte</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 */
	byte readByte(String name);

	/**
	 * Read the '<code>short</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	short readShort(int index);

	/**
	 * Read the '<code>short</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	short readShort(String name);

	/**
	 * Read the '<code>int</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	int readInt(int index);

	/**
	 * Read the '<code>int</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	int readInt(String name);

	/**
	 * Read the '<code>int</code>' value at index '<code>index</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param index the field index..
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	int readInt(int index, int defaultValue);

	/**
	 * Read the '<code>int</code>' value from column with given '<code>name</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	int readInt(String name, int defaultValue);

	/**
	 * Read the '<code>long</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	long readLong(int index);

	/**
	 * Read the '<code>long</code>' value from column with given '<code>name</code>'.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	long readLong(String name);

	/**
	 * Read the '<code>long</code>' value at index '<code>index</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param index the field index..
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	long readLong(int index, long defaultValue);

	/**
	 * Read the '<code>long</code>' value from column with given '<code>name</code>',
	 * using the supplied <code>defaultValue</code> if the field value is
	 * blank.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	long readLong(String name, long defaultValue);

	/**
	 * Read the '<code>float</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	float readFloat(int index);

	/**
	 * Read the '<code>float</code>' value from column with given '<code>name</code>.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	float readFloat(String name);

	/**
	 * Read the '<code>double</code>' value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	double readDouble(int index);

	/**
	 * Read the '<code>double</code>' value from column with given '<code>name</code>.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	double readDouble(String name);

	/**
	 * Read the {@link java.math.BigDecimal} value at index '<code>index</code>'.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	BigDecimal readBigDecimal(int index);

	/**
	 * Read the {@link java.math.BigDecimal} value from column with given '<code>name</code>.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	BigDecimal readBigDecimal(String name);

	/**
	 * Read the {@link BigDecimal} value at index '<code>index</code>',
	 * returning the supplied <code>defaultValue</code> if the trimmed string
	 * value at index '<code>index</code>' is blank.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 */
	BigDecimal readBigDecimal(int index, BigDecimal defaultValue);

	/**
	 * Read the {@link BigDecimal} value from column with given '<code>name</code>,
	 * returning the supplied <code>defaultValue</code> if the trimmed string
	 * value at index '<code>index</code>' is blank.
	 * 
	 * @param name the field name.
	 * @param defaultValue the default value to use if the field is blank
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	BigDecimal readBigDecimal(String name, BigDecimal defaultValue);

	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index the field index.
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 * @throws IllegalArgumentException if the value is not parseable
	 * @throws NullPointerException if the value is empty
	 */
	Date readDate(int index);

	/**
	 * Read the <code>java.sql.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name the field name.
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined or if the value is not parseable
	 * @throws NullPointerException if the value is empty
	 */
	Date readDate(String name);

	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index the field index.
	 * @param defaultValue the default value to use if the field is blank
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 * @throws IllegalArgumentException if the value is not parseable
	 * @throws NullPointerException if the value is empty
	 */
	Date readDate(int index, Date defaultValue);

	/**
	 * Read the <code>java.sql.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name the field name.
	 * @param defaultValue the default value to use if the field is blank
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined.
	 */
	Date readDate(String name, Date defaultValue);

	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index the field index.
	 * @param pattern the pattern describing the date and time format
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 * @throws IllegalArgumentException if the date cannot be parsed.
	 * 
	 */
	Date readDate(int index, String pattern);

	/**
	 * Read the <code>java.sql.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name the field name.
	 * @param pattern the pattern describing the date and time format
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined or if the specified field cannot be parsed
	 * 
	 */
	Date readDate(String name, String pattern);

	/**
	 * Read the <code>java.util.Date</code> value in default format at
	 * designated column <code>index</code>.
	 * 
	 * @param index the field index.
	 * @param pattern the pattern describing the date and time format
	 * @param defaultValue the default value to use if the field is blank
	 * @throws IndexOutOfBoundsException if the index is out of bounds.
	 * @throws IllegalArgumentException if the date cannot be parsed.
	 * 
	 */
	Date readDate(int index, String pattern, Date defaultValue);

	/**
	 * Read the <code>java.sql.Date</code> value in given format from column
	 * with given <code>name</code>.
	 * 
	 * @param name the field name.
	 * @param pattern the pattern describing the date and time format
	 * @param defaultValue the default value to use if the field is blank
	 * @throws IllegalArgumentException if a column with given name is not
	 * defined or if the specified field cannot be parsed
	 * 
	 */
	Date readDate(String name, String pattern, Date defaultValue);

	/**
	 * Return the number of fields in this '<code>FieldSet</code>'.
	 */
	int getFieldCount();

	/**
	 * Construct name-value pairs from the field names and string values. Null
	 * values are omitted.
	 * 
	 * @return some properties representing the field set.
	 * 
	 * @throws IllegalStateException if the field name meta data is not
	 * available.
	 */
	Properties getProperties();

}