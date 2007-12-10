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

package org.springframework.batch.io.exception;

/**
 * This exception is thrown when there is a critical configuration error and the
 * current job or module execution cannot continue.
 * 
 * @author Kerry O'Brien
 */
public class BatchConfigurationException extends BatchCriticalException {
	private static final long serialVersionUID = 759498454063502984L;

	/**
	 * @param msg
	 * @param ex
	 */
	public BatchConfigurationException(String msg, Throwable ex) {
		super(msg, ex);
	}

	/**
	 * @param msg
	 */
	public BatchConfigurationException(String msg) {
		super(msg);
	}

	/**
	 * @param nested
	 */
	public BatchConfigurationException(Throwable nested) {
		super(nested);
	}
}
