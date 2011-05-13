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

package org.springframework.batch.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;
import org.springframework.util.StringUtils;

/**
 * Unit tests for {@link PropertiesConverter}
 * 
 * @author Robert Kasanicky
 */
public class PropertiesConverterTests {
	
	//convenience attributes for storing results of conversions
	private Properties props = null;
	private String string = null;
	
	/**
	 * Check that Properties can be converted to String and back correctly.
	 */
	@Test
	public void testTwoWayRegularConversion() {
		
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
	public void testRegularConversionWithComma() {
		
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
	public void testRegularConversionWithCommaAndWhitespace() {
		
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
	public void testShortConversionWithCommas() {
		
		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");
		
		String value = PropertiesConverter.propertiesToString(storedProps);
		
		assertTrue("Wrong value: "+value, value.contains("key1=value1"));
		assertTrue("Wrong value: "+value, value.contains("key2=value2"));
		assertEquals(1, StringUtils.countOccurrencesOf(value, ","));
	}

	/**
	 * Check that Properties can be newline delimited.
	 */
	@Test
	public void testRegularConversionWithCommaAndNewline() {
		
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
	public void testStringToPropertiesNull() {
		props = PropertiesConverter.stringToProperties(null);
		assertNotNull(props);
		assertEquals("properties are empty", 0, props.size());
	}
	
	/**
	 * Null or empty properties should be converted to empty String
	 */
	@Test
	public void testPropertiesToStringNull() {
		string = PropertiesConverter.propertiesToString(null);
		assertEquals("", string);
		
		string = PropertiesConverter.propertiesToString(new Properties());
		assertEquals("", string);
	}
	
	@Test
	public void testEscapedColon() throws Exception {
		Properties props = new Properties();
		props.setProperty("test", "C:/test");
		String str = PropertiesConverter.propertiesToString(props);
		props = PropertiesConverter.stringToProperties(str);
		assertEquals("C:/test", props.getProperty("test"));
	}
	
}
