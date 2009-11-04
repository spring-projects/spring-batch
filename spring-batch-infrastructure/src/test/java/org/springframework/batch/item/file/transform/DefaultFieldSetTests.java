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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

public class DefaultFieldSetTests {

	DefaultFieldSet fieldSet;

	String[] tokens;

	String[] names;

	@Before
	public void setUp() throws Exception {

		tokens = new String[] { "TestString", "true", "C", "10", "-472", "354224", "543", "124.3", "424.3", "1,3245",
				null, "2007-10-12", "12-10-2007", "" };
		names = new String[] { "String", "Boolean", "Char", "Byte", "Short", "Integer", "Long", "Float", "Double",
				"BigDecimal", "Null", "Date", "DatePattern", "BlankInput" };

		fieldSet = new DefaultFieldSet(tokens, names);
		assertEquals(14, fieldSet.getFieldCount());

	}

	@Test
	public void testNames() throws Exception {
		assertTrue(fieldSet.hasNames());
		assertEquals(fieldSet.getFieldCount(), fieldSet.getNames().length);
	}

	@Test
	public void testNamesNotKnown() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] { "foo" });
		assertFalse(fieldSet.hasNames());
		try {
			fieldSet.getNames();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testReadString() throws ParseException {

		assertEquals(fieldSet.readString(0), "TestString");
		assertEquals(fieldSet.readString("String"), "TestString");

	}

	@Test
	public void testReadChar() throws Exception {

		assertTrue(fieldSet.readChar(2) == 'C');
		assertTrue(fieldSet.readChar("Char") == 'C');

	}

	@Test
	public void testReadBooleanTrue() throws Exception {

		assertTrue(fieldSet.readBoolean(1));
		assertTrue(fieldSet.readBoolean("Boolean"));

	}

	@Test
	public void testReadByte() throws Exception {

		assertTrue(fieldSet.readByte(3) == 10);
		assertTrue(fieldSet.readByte("Byte") == 10);

	}

	@Test
	public void testReadShort() throws Exception {

		assertTrue(fieldSet.readShort(4) == -472);
		assertTrue(fieldSet.readShort("Short") == -472);

	}

	@Test
	public void testReadIntegerAsFloat() throws Exception {

		assertEquals(354224, fieldSet.readFloat(5), .001);
		assertEquals(354224, fieldSet.readFloat("Integer"), .001);

	}

	@Test
	public void testReadFloat() throws Exception {

		assertTrue(fieldSet.readFloat(7) == 124.3F);
		assertTrue(fieldSet.readFloat("Float") == 124.3F);

	}

	@Test
	public void testReadIntegerAsDouble() throws Exception {

		assertEquals(354224, fieldSet.readDouble(5), .001);
		assertEquals(354224, fieldSet.readDouble("Integer"), .001);

	}

	@Test
	public void testReadDouble() throws Exception {

		assertTrue(fieldSet.readDouble(8) == 424.3);
		assertTrue(fieldSet.readDouble("Double") == 424.3);

	}

	@Test
	public void testReadBigDecimal() throws Exception {

		BigDecimal bd = new BigDecimal("424.3");
		assertEquals(bd, fieldSet.readBigDecimal(8));
		assertEquals(bd, fieldSet.readBigDecimal("Double"));

	}

	@Test
	public void testReadBigBigDecimal() throws Exception {

		fieldSet = new DefaultFieldSet(new String[] {"12345678901234567890"});
		BigDecimal bd = new BigDecimal("12345678901234567890");
		assertEquals(bd, fieldSet.readBigDecimal(0));

	}

	@Test
	public void testReadBigDecimalWithFormat() throws Exception {

		fieldSet.setNumberFormat(NumberFormat.getInstance(Locale.US));
		BigDecimal bd = new BigDecimal("424.3");
		assertEquals(bd, fieldSet.readBigDecimal(8));

	}

	@Test
	public void testReadBigDecimalWithEuroFormat() throws Exception {

		fieldSet.setNumberFormat(NumberFormat.getInstance(Locale.GERMANY));
		BigDecimal bd = new BigDecimal("1.3245");
		assertEquals(bd, fieldSet.readBigDecimal(9));

	}

	@Test
	public void testReadBigDecimalWithDefaultvalue() throws Exception {

		BigDecimal bd = new BigDecimal(324);
		assertEquals(bd, fieldSet.readBigDecimal(10, bd));
		assertEquals(bd, fieldSet.readBigDecimal("Null", bd));

	}

	@Test
	public void testReadNonExistentField() throws Exception {

		try {
			fieldSet.readString("something");
			fail("field set returns value even value was never put in!");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("something") > 0);
		}

	}

	@Test
	public void testReadIndexOutOfRange() throws Exception {

		try {
			fieldSet.readShort(-1);
			fail("field set returns value even index is out of range!");
		}
		catch (IndexOutOfBoundsException e) {
			assertTrue(true);
		}

		try {
			fieldSet.readShort(99);
			fail("field set returns value even index is out of range!");
		}
		catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testReadBooleanWithTrueValue() {
		assertTrue(fieldSet.readBoolean(1, "true"));
		assertFalse(fieldSet.readBoolean(1, "incorrect trueValue"));

		assertTrue(fieldSet.readBoolean("Boolean", "true"));
		assertFalse(fieldSet.readBoolean("Boolean", "incorrect trueValue"));
	}

	@Test
	public void testReadBooleanFalse() {
		fieldSet = new DefaultFieldSet(new String[] { "false" });
		assertFalse(fieldSet.readBoolean(0));
	}

	@Test
	public void testReadCharException() {
		try {
			fieldSet.readChar(1);
			fail("the value read was not a character, exception expected");
		}
		catch (IllegalArgumentException expected) {
			assertTrue(true);
		}

		try {
			fieldSet.readChar("Boolean");
			fail("the value read was not a character, exception expected");
		}
		catch (IllegalArgumentException expected) {
			assertTrue(true);
		}
	}

	@Test
	public void testReadInt() throws Exception {
		assertEquals(354224, fieldSet.readInt(5));
		assertEquals(354224, fieldSet.readInt("Integer"));
	}

	@Test
	public void testReadIntWithSeparator() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] {"354,224"});
		assertEquals(354224, fieldSet.readInt(0));
	}

	@Test
	public void testReadIntWithSeparatorAndFormat() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] {"354.224"});
		fieldSet.setNumberFormat(NumberFormat.getInstance(Locale.GERMAN));
		assertEquals(354224, fieldSet.readInt(0));
	}

	@Test
	public void testReadBlankInt() {

		// Trying to parse a blank field as an integer, but without a default
		// value should throw a NumberFormatException
		try {
			fieldSet.readInt(13);
			fail();
		}
		catch (NumberFormatException ex) {
			// expected
		}

		try {
			fieldSet.readInt("BlankInput");
			fail();
		}
		catch (NumberFormatException ex) {
			// expected
		}

	}

	@Test
	public void testReadLong() throws Exception {
		assertEquals(543, fieldSet.readLong(6));
		assertEquals(543, fieldSet.readLong("Long"));
	}

	@Test
	public void testReadLongWithPadding() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] { "000009" });
		assertEquals(9, fieldSet.readLong(0));
	}

	@Test
	public void testReadIntWithNullValue() {
		assertEquals(5, fieldSet.readInt(10, 5));
		assertEquals(5, fieldSet.readInt("Null", 5));
	}

	@Test
	public void testReadIntWithDefaultAndNotNull() throws Exception {
		assertEquals(354224, fieldSet.readInt(5, 5));
		assertEquals(354224, fieldSet.readInt("Integer", 5));
	}

	@Test
	public void testReadLongWithNullValue() {
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
	public void testReadBigDecimalInvalid() {
		int index = 0;

		try {
			fieldSet.readBigDecimal(index);
			fail("field value is not a number, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
		}

	}

	@Test
	public void testReadBigDecimalByNameInvalid() throws Exception {
		try {
			fieldSet.readBigDecimal("String");
			fail("field value is not a number, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
			assertTrue(e.getMessage().indexOf("name: [String]") > 0);
		}
	}

	@Test
	public void testReadDate() throws Exception {
		assertNotNull(fieldSet.readDate(11));
		assertNotNull(fieldSet.readDate("Date"));
	}

	@Test
	public void testReadDateWithDefault() throws Exception {
		Date date = null;
		assertEquals(date, fieldSet.readDate(13, date));
		assertEquals(date, fieldSet.readDate("BlankInput", date));
	}

	@Test
	public void testReadDateWithFormat() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] {"13/01/1999"});
		SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");
		fieldSet.setDateFormat(dateFormat);
		assertEquals(dateFormat.parse("13/01/1999"), fieldSet.readDate(0));
	}

	@Test
	public void testReadDateInvalid() throws Exception {

		try {
			fieldSet.readDate(0);
			fail("field value is not a date, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("TestString") > 0);
		}

	}

	@Test
	public void testReadDateInvalidByName() throws Exception {

		try {
			fieldSet.readDate("String");
			fail("field value is not a date, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("name: [String]") > 0);
		}

	}

	@Test
	public void testReadDateInvalidWithPattern() throws Exception {

		try {
			fieldSet.readDate(0, "dd-MM-yyyy");
			fail("field value is not a date, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("dd-MM-yyyy") > 0);
		}
	}

	@Test
	public void testReadDateWithPatternAndDefault() throws Exception {
		Date date = null;
		assertEquals(date, fieldSet.readDate(13, "dd-MM-yyyy", date));
		assertEquals(date, fieldSet.readDate("BlankInput", "dd-MM-yyyy", date));
	}

	@Test
	public void testStrictReadDateWithPattern() throws Exception {

		fieldSet = new DefaultFieldSet(new String[] {"50-2-13"});
		try {
			fieldSet.readDate(0, "dd-MM-yyyy");
			fail("field value is not a valid date for strict parser, exception expected");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message did not contain: " + message, message.indexOf("dd-MM-yyyy") > 0);
		}
	}

	@Test
	public void testStrictReadDateWithPatternAndStrangeDate() throws Exception {

		fieldSet = new DefaultFieldSet(new String[] {"5550212"});
		try {
			System.err.println(fieldSet.readDate(0, "yyyyMMdd"));
			fail("field value is not a valid date for strict parser, exception expected");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Message did not contain: " + message, message.indexOf("yyyyMMdd") > 0);
		}
	}

	@Test
	public void testReadDateByNameInvalidWithPattern() throws Exception {

		try {
			fieldSet.readDate("String", "dd-MM-yyyy");
			fail("field value is not a date, exception expected");
		}
		catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().indexOf("dd-MM-yyyy") > 0);
			assertTrue(e.getMessage().indexOf("String") > 0);
		}
	}

	@Test
	public void testEquals() {

		assertEquals(fieldSet, fieldSet);
		assertEquals(fieldSet, new DefaultFieldSet(tokens));

		String[] tokens1 = new String[] { "token1" };
		String[] tokens2 = new String[] { "token1" };
		FieldSet fs1 = new DefaultFieldSet(tokens1);
		FieldSet fs2 = new DefaultFieldSet(tokens2);
		assertEquals(fs1, fs2);
	}

	@Test
	public void testNullField() {
		assertEquals(null, fieldSet.readString(10));
	}

	@Test
	public void testEqualsNull() {
		assertFalse(fieldSet.equals(null));
	}

	@Test
	public void testEqualsNullTokens() {
		assertFalse(new DefaultFieldSet(null).equals(fieldSet));
	}

	@Test
	public void testEqualsNotEqual() throws Exception {

		String[] tokens1 = new String[] { "token1" };
		String[] tokens2 = new String[] { "token1", "token2" };
		FieldSet fs1 = new DefaultFieldSet(tokens1);
		FieldSet fs2 = new DefaultFieldSet(tokens2);
		assertFalse(fs1.equals(fs2));

	}

	@Test
	public void testHashCode() throws Exception {
		assertEquals(fieldSet.hashCode(), new DefaultFieldSet(tokens).hashCode());
	}

	@Test
	public void testHashCodeWithNullTokens() throws Exception {
		assertEquals(0, new DefaultFieldSet(null).hashCode());
	}

	@Test
	public void testConstructor() throws Exception {
		try {
			new DefaultFieldSet(new String[] { "1", "2" }, new String[] { "a" });
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testToStringWithNames() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" }, new String[] { "Foo", "Bar" });
		assertTrue(fieldSet.toString().indexOf("Foo=foo") >= 0);
	}

	@Test
	public void testToStringWithoutNames() throws Exception {
		fieldSet = new DefaultFieldSet(new String[] { "foo", "bar" });
		assertTrue(fieldSet.toString().indexOf("foo") >= 0);
	}

	@Test
	public void testToStringNullTokens() throws Exception {
		fieldSet = new DefaultFieldSet(null);
		assertEquals("", fieldSet.toString());
	}

	@Test
	public void testProperties() throws Exception {
		assertEquals("foo", new DefaultFieldSet(new String[] { "foo", "bar" }, new String[] { "Foo", "Bar" })
				.getProperties().getProperty("Foo"));
	}

	@Test
	public void testPropertiesWithNoNames() throws Exception {
		try {
			new DefaultFieldSet(new String[] { "foo", "bar" }).getProperties();
			fail("Expected IllegalStateException");
		}
		catch (IllegalStateException e) {
			// expected
		}
	}

	@Test
	public void testPropertiesWithWhiteSpace() throws Exception {

		assertEquals("bar", new DefaultFieldSet(new String[] { "foo", "bar   " }, new String[] { "Foo", "Bar" })
				.getProperties().getProperty("Bar"));
	}

	@Test
	public void testPropertiesWithNullValues() throws Exception {

		fieldSet = new DefaultFieldSet(new String[] { null, "bar" }, new String[] { "Foo", "Bar" });
		assertEquals("bar", fieldSet.getProperties().getProperty("Bar"));
		assertEquals(null, fieldSet.getProperties().getProperty("Foo"));
	}

	@Test
	public void testAccessByNameWhenNamesMissing() throws Exception {
		try {
			new DefaultFieldSet(new String[] { "1", "2" }).readInt("a");
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testGetValues() {
		String[] values = fieldSet.getValues();
		assertEquals(tokens.length, values.length);
		for (int i = 0; i < tokens.length; i++) {
			assertEquals(tokens[i], values[i]);
		}
	}

	@Test
	public void testPaddedLong() {
		FieldSet fs = new DefaultFieldSet(new String[] { "00000009" });

		long value = fs.readLong(0);
		assertEquals(value, 9);
	}

	@Test
	public void testReadRawString() {
		String name = "fieldName";
		String value = " string with trailing whitespace   ";
		FieldSet fs = new DefaultFieldSet(new String[] { value }, new String[] { name });

		assertEquals(value, fs.readRawString(0));
		assertEquals(value, fs.readRawString(name));
	}
}
