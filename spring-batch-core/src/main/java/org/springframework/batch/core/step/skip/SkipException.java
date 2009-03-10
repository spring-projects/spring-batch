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
package org.springframework.batch.core.step.skip;

import org.springframework.batch.core.UnexpectedJobExecutionException;

/**
 * Base exception indicating that the skip has failed or caused a failure.
 * 
 * @author Dave Syer
 */
public abstract class SkipException extends UnexpectedJobExecutionException {

	/**
	 * @param msg the message
	 * @param nested the cause
	 */
	public SkipException(String msg, Throwable nested) {
		super(msg, nested);
	}

	/**
	 * @param msg the message
	 */
	public SkipException(String msg) {
		super(msg);
	}
	
	

}
