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

import java.util.Properties;

import junit.framework.TestCase;

/**
 * Unit tests for {@link PropertiesConverter}
 * 
 * @author Robert Kasanicky
 */
public class PropertiesConverterTests extends TestCase {
	
	//convenience attributes for storing results of conversions
	private Properties props = null;
	private String string = null;
	
	/**
	 * Check that Properties can be converted to String and back correctly.
	 */
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
	public void testRegularConversionWithCommaAndWhitespace() {
		
		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");
		
		props = PropertiesConverter.stringToProperties("key1=value1, key2=value2");
		
		assertEquals(storedProps, props);
	}

	/**
	 * Check that Properties can be newline delimited.
	 */
	public void testRegularConversionWithCommaAndNewline() {
		
		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");
		
		props = PropertiesConverter.stringToProperties("key1=value1\n key2=value2");
		
		assertEquals(storedProps, props);
	}

	/**
	 * Converting a String to Properties and back does not return equal String!
	 * See {@link PropertiesConverter} javadoc for more details.
	 */
	public void testInvalidConversion() {
		String value = "key=value";
		string = PropertiesConverter.propertiesToString(PropertiesConverter.stringToProperties(value));
		assertFalse(value.equals(string));
	}
	
	/**
	 * Null String should be converted to empty Properties
	 */
	public void testStringToPropertiesNull() {
		props = PropertiesConverter.stringToProperties(null);
		assertNotNull(props);
		assertEquals("properties are empty", 0, props.size());
	}
	
	/**
	 * Null or empty properties should be converted to empty String
	 */
	public void testPropertiesToStringNull() {
		string = PropertiesConverter.propertiesToString(null);
		assertEquals("", string);
		
		string = PropertiesConverter.propertiesToString(new Properties());
		assertEquals("", string);
	}
	
}
