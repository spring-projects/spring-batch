/*
 * Copyright 2006-2019 the original author or authors.
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

import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * Abstract class handling common concerns of various {@link LineTokenizer}
 * implementations such as dealing with names and actual construction of
 * {@link FieldSet}
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Lucas Ward
 * @author Michael Minella
 */
public abstract class AbstractLineTokenizer implements LineTokenizer {

	protected String[] names = new String[0];

	private boolean strict = true;
	
	private String emptyToken = "";

	private FieldSetFactory fieldSetFactory = new DefaultFieldSetFactory();

	/**
	 * Public setter for the strict flag. If true (the default) then number of 
	 * tokens in line must match the number of tokens defined 
	 * (by {@link Range}, columns, etc.) in {@link LineTokenizer}. 
	 * If false then lines with less tokens will be tolerated and padded with 
	 * empty columns, and lines with more tokens will 
	 * simply be truncated.
	 * 
	 * @param strict the strict flag to set
	 */
	public void setStrict(boolean strict) {
		this.strict = strict;
	}
	
	/**
	 * Provides access to the strict flag for subclasses if needed.
	 * 
	 * @return the strict flag value
	 */
	protected boolean isStrict() {
		return strict;
	}
	
	/**
	 * Factory for {@link FieldSet} instances. Can be injected by clients to
	 * customize the default number and date formats.
	 * 
	 * @param fieldSetFactory the {@link FieldSetFactory} to set
	 */
	public void setFieldSetFactory(FieldSetFactory fieldSetFactory) {
		this.fieldSetFactory = fieldSetFactory;
	}

	/**
	 * Setter for column names. Optional, but if set, then all lines must have
	 * as many or fewer tokens.
	 * 
	 * @param names names of each column
	 */
	public void setNames(String... names) {
		if(names == null) {
			this.names = null;
		}
		else {
			boolean valid = false;
			for (String name : names) {
				if(StringUtils.hasText(name)) {
					valid = true;
					break;
				}
			}

			if(valid) {
				this.names = Arrays.asList(names).toArray(new String[names.length]);
			}
		}
	}

	/**
	 * @return <code>true</code> if column names have been specified
	 * @see #setNames(String[])
	 */
	public boolean hasNames() {
		if (names != null && names.length > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Yields the tokens resulting from the splitting of the supplied
	 * <code>line</code>.
	 * 
	 * @param line the line to be tokenized (can be <code>null</code>)
	 * 
	 * @return the resulting tokens
	 */
    @Override
	public FieldSet tokenize(@Nullable String line) {

		if (line == null) {
			line = "";
		}

		List<String> tokens = new ArrayList<>(doTokenize(line));
		
		// if names are set and strict flag is false
		if ( ( names.length != 0 ) && ( ! strict ) ) {
			adjustTokenCountIfNecessary( tokens );
		}
		
		String[] values = tokens.toArray(new String[tokens.size()]);

		if (names.length == 0) {
			return fieldSetFactory.create(values);
		}
		else if (values.length != names.length) {
			throw new IncorrectTokenCountException(names.length, values.length, line);
		}
		return fieldSetFactory.create(values, names);
	}

	protected abstract List<String> doTokenize(String line);
	
	/**
	 * Adds empty tokens or truncates existing token list to match expected 
	 * (configured) number of tokens in {@link LineTokenizer}.
	 * 
	 * @param tokens - list of tokens
	 */
	private void adjustTokenCountIfNecessary( List<String> tokens ) {
		
		int nameLength = names.length;
		int tokensSize = tokens.size();
		
		// if the number of tokens is not what expected
		if ( nameLength != tokensSize ) {
			
			if ( nameLength > tokensSize ) {

				// add empty tokens until the token list size matches
				// the expected number of tokens
				for ( int i = 0; i < ( nameLength - tokensSize ); i++ ) {
					tokens.add( emptyToken );
				}

			} else {
				// truncate token list to match the number of expected tokens
				for ( int i = tokensSize - 1; i >= nameLength; i-- ) {
					tokens.remove(i);
				}
			}
				
		}
	}
}
