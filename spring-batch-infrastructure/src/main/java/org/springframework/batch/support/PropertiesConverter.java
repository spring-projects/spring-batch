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
	 * @throws StringIOException
	 */
	public static Properties stringToProperties(String stringToParse) {

		if (stringToParse == null) {
			return new Properties();
		}

		if (!stringToParse.contains("\n")) {
			return StringUtils.splitArrayElementsIntoProperties(StringUtils
					.commaDelimitedListToStringArray(stringToParse), "=");
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
	 * @throws StringIOException if IOException is thrown from StringWriter
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

		return stringWriter.toString();
	}
}
