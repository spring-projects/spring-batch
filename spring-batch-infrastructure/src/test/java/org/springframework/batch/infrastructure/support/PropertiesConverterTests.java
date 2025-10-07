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

package org.springframework.batch.infrastructure.support;

import java.util.Properties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.batch.infrastructure.support.PropertiesConverter;
import org.springframework.util.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PropertiesConverter}.
 *
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 */
@SuppressWarnings("removal")
class PropertiesConverterTests {

	@Test
	void testStringToPropertiesConversion() {
		String stringToParse = "key1=value1\nkey2=value2";
		Properties expectedProperties = new Properties();
		expectedProperties.setProperty("key1", "value1");
		expectedProperties.setProperty("key2", "value2");

		Properties props = PropertiesConverter.stringToProperties(stringToParse);

		assertEquals(expectedProperties, props);
	}

	@Test
	void testPropertiesToStringConversion() {
		Properties properties = new Properties();
		properties.setProperty("key1", "value1");
		properties.setProperty("key2", "value2");

		String value = PropertiesConverter.propertiesToString(properties);

		assertTrue(value.contains("key1=value1"), "Wrong value: " + value);
		assertTrue(value.contains("key2=value2"), "Wrong value: " + value);
		assertEquals(1, StringUtils.countOccurrencesOf(value, "\n"));
	}

	@Test
	void testTwoWayRegularConversion() {
		Properties storedProps = new Properties();
		storedProps.setProperty("key1", "value1");
		storedProps.setProperty("key2", "value2");

		Properties props = PropertiesConverter.stringToProperties(PropertiesConverter.propertiesToString(storedProps));

		assertEquals(storedProps, props);
	}

	@Test
	void nullStringShouldNotBeAccepted() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> PropertiesConverter.stringToProperties(null));
	}

	@Test
	void emptyStringShouldBeConvertedToEmptyProperties() {
		Properties properties = PropertiesConverter.stringToProperties("");
		Assertions.assertTrue(properties.isEmpty());
	}

	@Test
	void nullPropertiesShouldNotBeAccepted() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> PropertiesConverter.propertiesToString(null));
	}

	@Test
	void emptyPropertiesShouldBeConvertedToEmptyString() {
		String string = PropertiesConverter.propertiesToString(new Properties());
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
