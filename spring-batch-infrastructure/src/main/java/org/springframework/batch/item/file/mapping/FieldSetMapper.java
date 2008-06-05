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

package org.springframework.batch.item.file.mapping;

/**
 * Interface that is used to map data obtained from a {@link FieldSet} into an
 * object.
 * 
 * @author tomas.slanina
 * @author Dave Syer
 * 
 */
public interface FieldSetMapper {

	/**
	 * Constant (negative) value indicating an unknown row number.
	 */
	public final static int ROW_NUMBER_UNKNOWN = -1;

	/**
	 * Method used to map data obtained from a {@link FieldSet} into an object.
	 * Implementations can do whatever they need to to convert the input into an
	 * object of the desired type. Often the row number is not used, and
	 * sometimes it is not available anyway, in which case its value will be
	 * {@link #ROW_NUMBER_UNKNOWN}.
	 * 
	 * @param fs the {@link FieldSet} to map
	 * @param rownum the row number for the field set in the input source (if
	 * known)
	 */
	public Object mapLine(FieldSet fs, int rownum);
}
