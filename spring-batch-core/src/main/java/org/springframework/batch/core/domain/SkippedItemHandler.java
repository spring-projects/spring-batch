/*
 * Copyright 2006-2008 the original author or authors.
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
package org.springframework.batch.core.domain;

import java.util.List;

import org.springframework.batch.io.exception.WriteFailureException;

/**
 * Interface for defining the contract to log out read and write
 * failures encountered during batch processing.  It is expected
 * that any failures encountered while writing will be
 * represented as {@link WriteFailureException}s, which contain
 * the items which caused the exception.
 * 
 * @author Lucas Ward
 *
 */
public interface SkippedItemHandler {
	
	/**
	 * Handler the list of exceptions.  This will usually be done
	 * by logging out the details of the exception to either a
	 * file or database table.  It is expected that any implementors of this
	 * of this method will not throw an exception.
	 * 
	 * @param exceptions
	 */
	void handle(List exceptions);

}
