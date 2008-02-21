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

/**
 * Interface for defining the contract to log out read and write
 * failures encountered during batch processing. 
 * 
 * @author Lucas Ward
 */
public interface ItemFailureHandler {
	
	/**
	 * Handle read failure.  This will usually be done
	 * by logging out the details of the exception to either a
	 * file or database table.  It is expected that any implementors of this
	 * of this method will not throw an exception.
	 * 
	 * @param ex
	 */
	void handleReadFailure(Exception ex);
	
	/**
	 * Handle read failure.  This will usually be done
	 * by logging out the details of the exception to either a
	 * file or database table.  It is expected that any implementors of this
	 * of this method will not throw an exception.
	 * 
	 * @param ex
	 */
	void handleWriteFailure(Object item, Exception ex);

}
