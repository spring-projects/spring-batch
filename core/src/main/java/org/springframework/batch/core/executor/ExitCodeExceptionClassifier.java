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
package org.springframework.batch.core.executor;

import org.springframework.batch.common.ExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;

/**
 * Extension of the ExceptionClassifier that explicitly deals with
 * returns an ExitStatus.  This is useful for mapping from an exception
 * type to an Exit Code with a detailed message.
 * 
 * @author Lucas Ward
 *
 */
public interface ExitCodeExceptionClassifier extends ExceptionClassifier {

	static final String FATAL_EXCEPTION = "FATAL_EXCEPTION";
	
	/**
	 * Typesafe version of classify that explicitly returns an {@link ExitStatus}
	 * object.
	 * 
	 * @param throwable
	 * @return ExitStatus representing the ExitCode and Message for the given
	 * exception.
	 */
	public ExitStatus classifyForExitCode(Throwable throwable);
}
