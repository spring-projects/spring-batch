/*
 * Copyright 2006-2007 the original author or authors.
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

package org.springframework.batch.item.file.transform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * This is a field extractor for a java bean. Given an array of property names,
 * a delimiter and a quote character, it will reflectively call getters on the
 * item and return an array of all the values with eventually the quote character.
 * 
 * @author Dan Garrette
 * @author ADANSAR Mohamed
 * @since 2.0
 */
public class BeanWrapperFieldExtractor<T> implements FieldExtractor<T>, InitializingBean {

	/**
	 * Convenient constant for the common case of a comma delimiter.
	 */
	public static final String DELIMITER_COMMA = ",";
	
	/**
	 * Convenient constant for the common case of a " character used to escape delimiters or line endings.
	 */
	public static final char DEFAULT_QUOTE_CHARACTER = '"';

	// the delimiter character used when reading input.
	private String delimiter;

	private char quoteCharacter = DEFAULT_QUOTE_CHARACTER;

	private String quoteString;

	private String[] names;

	/**
	 * Create a new instance of the {@link BeanWrapperFieldExtractorWithDelimiter} class for the common case where the delimiter is a
	 * {@link #DELIMITER_COMMA comma}.
	 *
	 * @see #BeanWrapperFieldExtractor(String)
	 * @see #DELIMITER_COMMA
	 */
	public BeanWrapperFieldExtractorWithDelimiter() {
		this(DELIMITER_COMMA);
	}

	/**
	 * Create a new instance of the {@link BeanWrapperFieldExtractorWithDelimiter} class.
	 *
	 * @param delimiter
	 *            the desired delimiter
	 */
	public BeanWrapperFieldExtractorWithDelimiter(String delimiter) {
		this.delimiter = delimiter;
		setQuoteCharacter(DEFAULT_QUOTE_CHARACTER);
	}

	/**
	 * Setter for the quoteCharacter. The quote character can be used to extend a field across line endings or to enclose a String which contains the
	 * delimiter.
	 *
	 * @param quoteCharacter
	 *            the quoteCharacter to set
	 *
	 */
	public void setQuoteCharacter(char quoteCharacter) {
		this.quoteCharacter = quoteCharacter;
		this.quoteString = "" + quoteCharacter;
	}
	
	/**
	 * Setter for the delimiter character.
  	 *
	 * @param delimiter
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
	
	/**
	 * @param names field names to be extracted by the {@link #extract(Object)} method.
	 */
	public void setNames(String[] names) {
		Assert.notNull(names, "Names must be non-null");
		this.names = Arrays.asList(names).toArray(new String[names.length]);
	}

	/**
	 * @see org.springframework.batch.item.file.transform.FieldExtractor#extract(java.lang.Object)
	 */
    @Override
	public Object[] extract(T item) {
		List<Object> values = new ArrayList<>();

		BeanWrapper bw = new BeanWrapperImpl(item);
		for (String propertyName : this.names) {
			Object myObject = bw.getPropertyValue(propertyName);
			if (myObject instanceof String && ((String) myObject).contains(delimiter)) {
				values.add(quoteString + myObject + quoteString);
			} else {
				values.add(myObject);
          		}
		}
		return values.toArray();
	}

    @Override
	public void afterPropertiesSet() {
		Assert.notNull(names, "The 'names' property must be set.");
	}
}
