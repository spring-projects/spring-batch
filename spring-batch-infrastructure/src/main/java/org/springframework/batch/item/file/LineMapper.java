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

package org.springframework.batch.item.file;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.LineTokenizer;


/**
 * Interface for mapping lines (strings) to domain objects typically used to map lines read from a file to domain objects
 * on a per line basis.  Implementations of this interface perform the actual
 * work of parsing a line without having to deal with how the line was
 * obtained.  
 * 
 * @author Robert Kasanicky
 * @param <T> type of the domain object
 * @see FieldSetMapper
 * @see LineTokenizer
 * @since 2.0
 */
public interface LineMapper<T> {

	/**
	 * Implementations must implement this method to map the provided line to 
	 * the parameter type T.  The line number represents the number of lines
	 * into a file the current line resides.
	 * 
	 * @param line to be mapped
	 * @param lineNumber of the current line
	 * @return mapped object of type T
	 * @throws Exception if error occured while parsing.
	 */
	T mapLine(String line, int lineNumber) throws Exception;
}
