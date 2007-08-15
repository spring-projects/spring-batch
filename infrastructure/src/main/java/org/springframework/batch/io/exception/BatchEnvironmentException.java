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
 * Exception that should be thrown to indicate an error in the environment.
 * Excamples of such errors include file or database access errors. Because this
 * class extends BatchCriticalException, throwing this error will indicate to
 * the framework that processing should stop. It is vital that an error-code be
 * passed as well, since this will be returned from the main method of the
 * launcher.
 * 
 * @author Lucas Ward
 */
public class BatchEnvironmentException extends BatchCriticalException {
	private static final long serialVersionUID = 1382420837776529019L;

	/**
	 * Refer to the similar constructor in the parent class
	 * {@link BatchCriticalException}.
	 * 
	 */
	public BatchEnvironmentException(String msg, Throwable nested) {
		super(msg, nested);
	}

	/**
	 * Refer to the similar constructor in the parent class
	 * {@link BatchCriticalException}.
	 * 
	 */
	public BatchEnvironmentException(String msg) {
		super(msg);
	}

}
