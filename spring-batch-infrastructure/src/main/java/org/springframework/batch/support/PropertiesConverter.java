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

package org.springframework.batch.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Utility to convert a Properties object to a String and back. The format of properties
 * is new line separated key=value pairs.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 * @deprecated since 6.0 with no replacement. Scheduled for removal in 6.2 or later.
 */
@Deprecated(since = "6.0", forRemoval = true)
public final class PropertiesConverter {

	private static final String LINE_SEPARATOR = "\n";

	// prevents the class from being instantiated
	private PropertiesConverter() {
	}

	/**
	 * Parse a String to a Properties object. If string is empty, an empty Properties
	 * object will be returned. The input String should be a set of key=value pairs,
	 * separated by a new line.
	 * @param stringToParse String to parse. Must not be {@code null}.
	 * @return Properties parsed from each key=value pair.
	 */
	public static Properties stringToProperties(@NonNull String stringToParse) {
		Assert.notNull(stringToParse, "stringToParse must not be null");
		if (!StringUtils.hasText(stringToParse)) {
			return new Properties();
		}
		Properties properties = new Properties();
		String[] keyValuePairs = stringToParse.split(LINE_SEPARATOR);
		for (String string : keyValuePairs) {
			if (!string.contains("=")) {
				throw new IllegalArgumentException(string + "is not a valid key=value pair");
			}
			String[] keyValuePair = string.split("=");
			properties.setProperty(keyValuePair[0], keyValuePair[1]);
		}
		return properties;
	}

	/**
	 * Convert a Properties object to a String. This is only necessary for compatibility
	 * with converting the String back to a properties object. If an empty properties
	 * object is passed in, a blank string is returned, otherwise it's string
	 * representation is returned.
	 * @param propertiesToParse contains the properties to be converted. Must not be
	 * {@code null}.
	 * @return String representation of the properties object
	 */
	public static String propertiesToString(@NonNull Properties propertiesToParse) {
		Assert.notNull(propertiesToParse, "propertiesToParse must not be null");
		if (propertiesToParse.isEmpty()) {
			return "";
		}
		List<String> keyValuePairs = new ArrayList<>();
		for (Map.Entry<Object, Object> entry : propertiesToParse.entrySet()) {
			keyValuePairs.add(entry.getKey() + "=" + entry.getValue());
		}
		return String.join(LINE_SEPARATOR, keyValuePairs);
	}

}
