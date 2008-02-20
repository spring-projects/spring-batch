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

package org.springframework.batch.io.file.transform;

import org.springframework.batch.io.file.mapping.FieldSet;


/**
 * Class used to create string representing object. Values are separated by
 * defined delimiter.
 * 
 * @author tomas.slanina
 * 
 */
public class DelimitedLineAggregator implements LineAggregator {
	private String delimiter = ",";

	/**
	 * Method used to create string representing object.
	 * 
	 * @param fieldSet arrays of strings representing data to be stored
	 * @param lineDescriptor for this implementation this parameter is not
	 * used
	 */
	public String aggregate(FieldSet fieldSet) {
		StringBuffer buffer = new StringBuffer();
		String[] args = fieldSet.getValues();
		for (int i = 0; i < args.length; i++) {
			buffer.append(args[i]);

			if (i != (args.length - 1)) {
				buffer.append(delimiter);
			}
		}

		return buffer.toString();
	}

	/**
	 * Sets the character to be used as a delimiter.
	 */
	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}
}
