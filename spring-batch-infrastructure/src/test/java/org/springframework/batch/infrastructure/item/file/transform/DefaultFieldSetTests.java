/*
 * Copyright 2006-2023 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

class DefaultFieldSetTests {

	DefaultFieldSet fieldSet;

	String[] tokens;

	String[] names;

	@BeforeEach
	void setUp() {

		tokens = new String[] { "TestString", "true", "C", "10", "-472", "354224", "543", "124.3", "424.3", "1,3245",
				null, "2007-10-12", "12-10-2007", "" };
		names = new String[] { "String", "Boolean", "Char", "Byte", "Short", "Integer", "Long", "Float", "Double",
				"BigDecimal", "Null", "Date", "DatePattern", "BlankInput" };

		fieldSet = new DefaultFieldSet(tokens, names);
		assertEquals(14, fieldSet.getFieldCount());

	}

	@Test
	void testNames() {
		assertTrue(fieldSet.hasNames());
		assertEquals(fieldSet.getFieldCount(), fieldSet.getNames().length);
	}

	@Test
	void testNamesNotKnown() {
		fieldSet = new DefaultFieldSet(new String[] { "foo" });
		assertFalse(fieldSet.hasNames());
		assertThrows(IllegalStateException.class, fieldSet::getNames);
	}

	@Test
	void testReadString() {

		assertEquals(fieldSet.readString(0), "TestString");
		assertEquals(fieldSet.readString("String"), "TestString");

	}

	@Test
	void testReadChar() {

		assertEquals('C', fieldSet.readChar(2));
		assertEquals('C', fieldSet.readChar("Char"));

	}

	@Test
	void testReadBooleanTrue() {

		assertTrue(fieldSet.readBoolean(1));
		assertTrue(fieldSet.readBoolean("Boolean"));

	}

	@Test
	void testReadByte() {

		assertEquals(10, fieldSet.readByte(3));
		assertEquals(10, fieldSet.readByte("Byte"));

	}

	@Test
	void testReadShort() {

		assertEquals(-472, fieldSet.readShort(4));
		assertEquals(-472, fieldSet.readShort("Short"));

	}

	@Test
	void testReadIntegerAsFloat() {

		assertEquals(354224, fieldSet.readFloat(5), .001);
		assertEquals(354224, fieldSet.readFloat("Integer"), .001);

	}

	@Test
	void testReadFloat() {

		assertEquals(124.3F, fieldSet.readFloat(7));
		assertEquals(124.3F, fieldSet.readFloat("Float"));

	}

	@Test
	void testReadIntegerAsDouble() {

		assertEquals(354224, fieldSet.readDouble(5), .001);
		assertEquals(354224, fieldSet.readDouble("Integer"), .001);

	}

	@Test
	void testReadDouble() {

		assertEquals(424.3, fieldSet.readDouble(8));
		assertEquals(424.3, fieldSet.readDouble("Double"));

	}

	@Test
	void testReadBigDecimal() {

		BigDecimal bd = new BigDecimal("424.3");
		assertEquals(bd, fieldSet.readBigDecimal(8));
		assertEquals(bd, fieldSet.readBigDecimal("Double"));

	}

	@Test
	void testReadBigBigDecimal() {

		fieldSet = new DefaultFieldSet(new String[] { "12345678901234567890" });
		BigDecimal bd = new BigDecimal("12345678901234567890");
		assertEquals(bd, fieldSet.readBigDecimal(0));

	}

	@Test
	void testReadBigDecimalWithFormat() {

		fieldSet.setNumberFormat(NumberFormat.getInstance(Locale.US));
		BigDecimal bd = new BigDecimal("424.3");
		assertEquals(bd, fieldSet.readBigDecimal(8));

	}

	@Test
	void testReadBigDecimalWithEuroFormat() {

		fieldSet.setNumberFormat(NumberFormat.getInstance(Locale.GERMANY));
		BigDecimal bd = new BigDecimal("1.3245");
		assertEquals(bd, fieldSet.readBigDecimal(9));

	}

	@Test
	void testReadBigDecimalWithDefaultvalue() {

		BigDecimal bd = new BigDecimal(324);
		assertEquals(bd, fieldSet.readBigDecimal(10, bd));
		assertEquals(bd, fieldSet.readBigDecimal("Null", bd));

	}

	@Test
	void testReadNonExistentField() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readString("something"));
		assertTrue(exception.getMessage().contains("something"));
	}

	@Test
	void testReadIndexOutOfRange() {
		assertThrows(IndexOutOfBoundsException.class, () -> fieldSet.readShort(-1));
		assertThrows(Exception.class, () -> fieldSet.readShort(99));
	}

	@Test
	void testReadBooleanWithTrueValue() {
		assertTrue(fieldSet.readBoolean(1, "true"));
		assertFalse(fieldSet.readBoolean(1, "incorrect trueValue"));

		assertTrue(fieldSet.readBoolean("Boolean", "true"));
		assertFalse(fieldSet.readBoolean("Boolean", "incorrect trueValue"));
	}

	@Test
	void testReadBooleanFalse() {
		fieldSet = new DefaultFieldSet(new String[] { "false" });
		assertFalse(fieldSet.readBoolean(0));
	}

	@Test
	void testReadCharException() {
		assertThrows(IllegalArgumentException.class, () -> fieldSet.readChar(1));
		assertThrows(IllegalArgumentException.class, () -> fieldSet.readChar("Boolean"));
	}

	@Test
	void testReadInt() {
		assertEquals(354224, fieldSet.readInt(5));
		assertEquals(354224, fieldSet.readInt("Integer"));
	}

	@Test
	void testReadIntWithSeparator() {
		fieldSet = new DefaultFieldSet(new String[] { "354,224" });
		assertEquals(354224, fieldSet.readInt(0));
	}

	@Test
	void testReadIntWithSeparatorAndFormat() {
		fieldSet = new DefaultFieldSet(new String[] { "354.224" });
		fieldSet.setNumberFormat(NumberFormat.getInstance(Locale.GERMAN));
		assertEquals(354224, fieldSet.readInt(0));
	}

	@Test
	void testReadBlankInt() {
		// Trying to parse a blank field as an integer, but without a default
		// value should throw a NumberFormatException
		assertThrows(NumberFormatException.class, () -> fieldSet.readInt(13));
		assertThrows(NumberFormatException.class, () -> fieldSet.readInt("BlankInput"));
	}

	@Test
	void testReadLong() {
		assertEquals(543, fieldSet.readLong(6));
		assertEquals(543, fieldSet.readLong("Long"));
	}

	@Test
	void testReadLongWithPadding() {
		fieldSet = new DefaultFieldSet(new String[] { "000009" });
		assertEquals(9, fieldSet.readLong(0));
	}

	@Test
	void testReadIntWithNullValue() {
		assertEquals(5, fieldSet.readInt(10, 5));
		assertEquals(5, fieldSet.readInt("Null", 5));
	}

	@Test
	void testReadIntWithDefaultAndNotNull() {
		assertEquals(354224, fieldSet.readInt(5, 5));
		assertEquals(354224, fieldSet.readInt("Integer", 5));
	}

	@Test
	void testReadLongWithNullValue() {
		int defaultValue = 5;
		int indexOfNull = 10;
		int indexNotNull = 6;
		String nameNull = "Null";
		String nameNotNull = "Long";
		long longValueAtIndex = 543;

		assertEquals(fieldSet.readLong(indexOfNull, defaultValue), defaultValue);
		assertEquals(fieldSet.readLong(indexNotNull, defaultValue), longValueAtIndex);

		assertEquals(fieldSet.readLong(nameNull, defaultValue), defaultValue);
		assertEquals(fieldSet.readLong(nameNotNull, defaultValue), longValueAtIndex);
	}

	@Test
	void testReadBigDecimalInvalid() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readBigDecimal(0));
		assertTrue(exception.getMessage().contains("TestString"));
	}

	@Test
	void testReadBigDecimalByNameInvalid() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readBigDecimal("String"));
		String message = exception.getMessage();
		assertTrue(message.contains("TestString"));
		assertTrue(message.contains("name: [String]"));
	}

	@Test
	void testReadDate() {
		assertNotNull(fieldSet.readDate(11));
		assertNotNull(fieldSet.readDate("Date"));
	}

	@Test
	void testReadDateWithDefault() {
		Date date = null;
		assertEquals(date, fieldSet.readDate(13, date));
		assertEquals(date, fieldSet.readDate("BlankInput", date));
	}

	@Test
	void testReadDateWithFormat() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] { "13/01/1999" });
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		fieldSet.setDateFormat(dateFormat);
		assertEquals(dateFormat.parse("13/01/1999"), fieldSet.readDate(0));
	}

	@Test
	void testReadDateInvalid() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate(0));
		assertTrue(exception.getMessage().contains("TestString"));
	}

	@Test
	void testReadDateInvalidByName() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate("String"));
		assertTrue(exception.getMessage().contains("name: [String]"));
	}

	@Test
	void testReadDateInvalidWithPattern() {
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate(0, "dd-MM-yyyy"));
		assertTrue(exception.getMessage().contains("dd-MM-yyyy"));
	}

	@Test
	void testReadDateWithPatternAndDefault() {
		Date date = null;
		assertEquals(date, fieldSet.readDate(13, "dd-MM-yyyy", date));
		assertEquals(date, fieldSet.readDate("BlankInput", "dd-MM-yyyy", date));
	}

	@Test
	void testReadDateInvalidWithDefault() {
		Date defaultDate = new Date();
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate(1, defaultDate));
		assertTrue(exception.getMessage().contains("yyyy-MM-dd"));

		exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate("String", defaultDate));
		assertTrue(exception.getMessage().contains("yyyy-MM-dd"));
		assertTrue(exception.getMessage().contains("name: [String]"));

		exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate(1, "dd-MM-yyyy", defaultDate));
		assertTrue(exception.getMessage().contains("dd-MM-yyyy"));

		exception = assertThrows(IllegalArgumentException.class,
				() -> fieldSet.readDate("String", "dd-MM-yyyy", defaultDate));
		assertTrue(exception.getMessage().contains("dd-MM-yyyy"));
		assertTrue(exception.getMessage().contains("name: [String]"));
	}

	@Test
	void testStrictReadDateWithPattern() {
		fieldSet = new DefaultFieldSet(new String[] { "50-2-13" });
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate(0, "dd-MM-yyyy"));
		String message = exception.getMessage();
		assertTrue(message.contains("dd-MM-yyyy"), "Message did not contain: " + message);
	}

	@Test
	void testStrictReadDateWithPatternAndStrangeDate() {
		fieldSet = new DefaultFieldSet(new String[] { "5550212" });
		Exception exception = assertThrows(IllegalArgumentException.class, () -> fieldSet.readDate(0, "yyyyMMdd"));
		String message = exception.getMessage();
		assertTrue(message.contains("yyyyMMdd"), "Message did not contain: " + message);
	}

	@Test
	void testReadDateByNameInvalidWithPattern() {
		Exception exception = assertThrows(IllegalArgumentException.class,
				() -> fieldSet.readDate("String", "dd-MM-yyyy"));
		assertTrue(exception.getMessage().contains("dd-MM-yyyy"));
		assertTrue(exception.getMessage().contains("String"));
	}

	@Test
	void testEquals() {

		assertEquals(fieldSet, fieldSet);
		assertEquals(fieldSet, new DefaultFieldSet(tokens));

		String[] tokens1 = new String[] { "token1" };
		String[] tokens2 = new String[] { "token1" };
		FieldSet fs1 = new DefaultFieldSet(tokens1);
		FieldSet fs2 = new DefaultFieldSet(tokens2);
		assertEquals(fs1, fs2);
	}

	@Test
	void testNullField() {
		assertNull(fieldSet.readString(10));
	}

	@Test
	void testEqualsNull() {
		assertNotEquals(null, fieldSet);
	}

	@Test
	void testEqualsNullTokens() {
		assertNotEquals(new DefaultFieldSet(null), fieldSet);
	}

	@Test
	void testEqualsNotEqual() {

		String[] tokens1 = new String[] { "token1" };
		String[] tokens2 = new String[] { "token1", "token2" };
		FieldSet fs1 = new DefaultFieldSet(tokens1);
		FieldSet fs2 = new DefaultFieldSet(tokens2);
		assertNotEquals(fs1, fs2);

	}

	@Test
	void testHashCode() {
		assertEquals(fieldSet.hashCode(), new DefaultFieldSet(tokens).hashCode());
	}

	@Test
	void testHashCodeWithNullTokens() {
		assertEquals(0, new DefaultFieldSet(null).hashCode());
	}

	@Test
	void testConstructor() {
		assertThrows(IllegalArgumentException.class,
				() -> new DefaultFieldSet(new String[] { "1", "2" }, new String[] { "a" }));
	}

	@Test
	void testToStringWithNames() {
		fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" }, new String[] { "Foo", "Bar" });
		assertTrue(fieldSet.toString().contains("Foo=foo"));
	}

	@Test
	void testToStringWithoutNames() {
		fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertTrue(fieldSet.toString().contains("foo"));
	}

	@Test
	void testToStringNullTokens() {
		fieldSet = new DefaultFieldSet(null);
		assertEquals("[]", fieldSet.toString());
	}

	@Test
	void testProperties() {
		assertEquals("foo",
				new DefaultFieldSet(new String[] { "foo", "bar" }, new String[] { "Foo", "Bar" }).getProperties()
					.getProperty("Foo"));
	}

	@Test
	void testPropertiesWithNoNames() {
		assertThrows(IllegalStateException.class,
				() -> new DefaultFieldSet(new String[] { "foo", "bar" }).getProperties());
	}

	@Test
	void testPropertiesWithWhiteSpace() {

		assertEquals("bar",
				new DefaultFieldSet(new String[] { "foo", "bar   " }, new String[] { "Foo", "Bar" }).getProperties()
					.getProperty("Bar"));
	}

	@Test
	void testPropertiesWithNullValues() {

		fieldSet = new DefaultFieldSet(new String[] { null, "bar" }, new String[] { "Foo", "Bar" });
		assertEquals("bar", fieldSet.getProperties().getProperty("Bar"));
		assertNull(fieldSet.getProperties().getProperty("Foo"));
	}

	@Test
	void testAccessByNameWhenNamesMissing() {
		assertThrows(IllegalArgumentException.class, () -> new DefaultFieldSet(new String[] { "1", "2" }).readInt("a"));
	}

	@Test
	void testGetValues() {
		String[] values = fieldSet.getValues();
		assertEquals(tokens.length, values.length);
		for (int i = 0; i < tokens.length; i++) {
			assertEquals(tokens[i], values[i]);
		}
	}

	@Test
	void testPaddedLong() {
		FieldSet fs = new DefaultFieldSet(new String[] { "00000009" });

		long value = fs.readLong(0);
		assertEquals(value, 9);
	}

	@Test
	void testReadRawString() {
		String name = "fieldName";
		String value = " string with trailing whitespace   ";
		FieldSet fs = new DefaultFieldSet(new String[] { value }, new String[] { name });

		assertEquals(value, fs.readRawString(0));
		assertEquals(value, fs.readRawString(name));
	}

}
