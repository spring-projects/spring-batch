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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.springframework.util.DefaultPropertiesPersister;
import org.springframework.util.PropertiesPersister;
import org.springframework.util.StringUtils;

/**
 * Utility to convert a Properties object to a String and back. Ideally this
 * utility should have been used to convert to string in order to convert that
 * string back to a Properties Object. Attempting to convert a string obtained
 * by calling Properties.toString() will return an invalid Properties object.
 * The format of Properties is that used by {@link PropertiesPersister} from the
 * Spring Core, so a String in the correct format for a Spring property editor
 * is fine (key=value pairs separated by new lines).
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 * @see PropertiesPersister
 */
public final class PropertiesConverter {

	private static final PropertiesPersister propertiesPersister = new DefaultPropertiesPersister();

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	// prevents the class from being instantiated
	private PropertiesConverter() {
	};

	/**
	 * Parse a String to a Properties object. If string is null, an empty
	 * Properties object will be returned. The input String is a set of
	 * name=value pairs, delimited by either newline or comma (for brevity). If
	 * the input String contains a newline it is assumed that the separator is
	 * newline, otherwise comma.
	 * 
	 * @param stringToParse String to parse.
	 * @return Properties parsed from each string.
	 * @see PropertiesPersister
	 */
	public static Properties stringToProperties(String stringToParse) {

		if (stringToParse == null) {
			return new Properties();
		}

		if (!contains(stringToParse, "\n")) {
			stringToParse = StringUtils.arrayToDelimitedString(
					StringUtils.commaDelimitedListToStringArray(stringToParse), "\n");
		}

		StringReader stringReader = new StringReader(stringToParse);

		Properties properties = new Properties();

		try {
			propertiesPersister.load(properties, stringReader);
			// Exception is only thrown by StringReader after it is closed,
			// so never in this case.
		}
		catch (IOException ex) {
			throw new IllegalStateException("Error while trying to parse String to java.util.Properties,"
					+ " given String: " + properties);
		}

		return properties;
	}

	/**
	 * Convert Properties object to String. This is only necessary for
	 * compatibility with converting the String back to a properties object. If
	 * an empty properties object is passed in, a blank string is returned,
	 * otherwise it's string representation is returned.
	 * 
	 * @param propertiesToParse
	 * @return String representation of properties object
	 */
	public static String propertiesToString(Properties propertiesToParse) {

		// If properties is empty, return a blank string.
		if (propertiesToParse == null || propertiesToParse.size() == 0) {
			return "";
		}

		StringWriter stringWriter = new StringWriter();

		try {
			propertiesPersister.store(propertiesToParse, stringWriter, null);
		}
		catch (IOException ex) {
			// Exception is never thrown by StringWriter
			throw new IllegalStateException("Error while trying to convert properties to string");
		}

		// If the value is short enough (and doesn't contain commas), convert to
		// comma-separated...
		String value = stringWriter.toString();
		if (value.length() < 160) {
			List<String> list = Arrays.asList(StringUtils.delimitedListToStringArray(value, LINE_SEPARATOR,
					LINE_SEPARATOR));
			String shortValue = StringUtils.collectionToCommaDelimitedString(list.subList(1, list.size()));
			int count = StringUtils.countOccurrencesOf(shortValue, ",");
			if (count == list.size() - 2) {
				value = shortValue;
			}
			if (value.endsWith(",")) {
				value = value.substring(0, value.length() - 1);
			}
		}
		return value;
	}

	private static boolean contains(String str, String searchStr) {
		return str.indexOf(searchStr) != -1;
	}
}
