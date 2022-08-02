/*
 * Copyright 2006-2022 the original author or authors.
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

package org.springframework.batch.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link PropertiesConverter}
 *
 * @author Robert Kasanicky
 */
class PropertiesConverterTests {

	// convenience attributes for storing results of conversions
	private Properties props = null;

	/**
	 * Check that Properties can be converted to String and back correctly.
	 */
	@Test
	void testTwoWayRegularConversion() {

		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");

		props = PropertiesConverter.stringToProperties(PropertiesConverter.propertiesToString(storedProps));

		assertEquals(storedProps, props);
	}

	/**
	 * Check that Properties can be comma delimited.
	 */
	@Test
	void testRegularConversionWithComma() {

		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");

		props = PropertiesConverter.stringToProperties("key1=value1,key2=value2");

		assertEquals(storedProps, props);
	}

	/**
	 * Check that Properties can be comma delimited with extra whitespace.
	 */
	@Test
	void testRegularConversionWithCommaAndWhitespace() {

		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");

		props = PropertiesConverter.stringToProperties("key1=value1, key2=value2");

		assertEquals(storedProps, props);
	}

	/**
	 * Check that Properties can be comma delimited with extra whitespace.
	 */
	@Test
	void testShortConversionWithCommas() {

		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");

		String value = PropertiesConverter.propertiesToString(storedProps);

		assertTrue(value.contains("key1=value1"), "Wrong value: " + value);
		assertTrue(value.contains("key2=value2"), "Wrong value: " + value);
		assertEquals(1, StringUtils.countOccurrencesOf(value, ","));
	}

	/**
	 * Check that Properties can be newline delimited.
	 */
	@Test
	void testRegularConversionWithCommaAndNewline() {

		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");

		props = PropertiesConverter.stringToProperties("key1=value1\n key2=value2");

		assertEquals(storedProps, props);
	}

	/**
	 * Null String should be converted to empty Properties
	 */
	@Test
	void testStringToPropertiesNull() {
		props = PropertiesConverter.stringToProperties(null);
		assertNotNull(props);
		assertEquals(0, props.size(), "properties are empty");
	}

	/**
	 * Null or empty properties should be converted to empty String
	 */
	@Test
	void testPropertiesToStringNull() {
		String string = PropertiesConverter.propertiesToString(null);
		assertEquals("", string);

		string = PropertiesConverter.propertiesToString(new Properties());
		assertEquals("", string);
	}

	@Test
	void testEscapedColon() {
		Properties props = new Properties();
		props.setProperty("test", "C:/test");
		String str = PropertiesConverter.propertiesToString(props);
		props = PropertiesConverter.stringToProperties(str);
		assertEquals("C:/test", props.getProperty("test"));
	}

}
