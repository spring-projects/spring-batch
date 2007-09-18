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

package org.springframework.batch.execution.bootstrap;

import org.springframework.batch.repeat.ExitCodeMapper;


/**
 * Abstract class for mapping ExitCodes from the Batch framework to
 * JVM Return Codes
 *
 * @author Stijn Maller
 * @author Lucas Ward
 */

public interface JvmExitCodeMapper extends ExitCodeMapper {
	
	static int JVM_EXITCODE_COMPLETED = 0;
	static int JVM_EXITCODE_GENERIC_ERROR = 1;
	static int JVM_EXITCODE_NO_SUCH_JOBCONFIGURATION = 2;
	
	/**
	 * Transform the exitcode known by the batchframework into a JVM return
	 * value.(Must be of type int) 
	 * @param exitCode	The exitcode which is used internally by the batch framework.
	 * @return			The corresponding JVM return value
	 */
	public int getExitCode(String exitCode);
	
}
