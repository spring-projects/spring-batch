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

package org.springframework.batch.repeat;

/**
 * 
 * This interface should be implemented when an environment calling the batch famework has specific
 * requirements regarding the returncodes returned by the BatchFramework. 
 *
 * @param The type of returncode expected by the environment
 * @author Stijn Maller
 * @author Lucas Ward
 */
public interface ExitCodeMapper {

	String BATCH_EXITCODE_COMPLETED = ExitStatus.FINISHED.getExitCode();
	String BATCH_EXITCODE_GENERIC_ERROR = ExitStatus.FAILED.getExitCode();
	String BATCH_EXITCODE_NO_SUCH_JOBCONFIGURATION = "NO_SUCH_JOBCONFIGURATION";

	/**
	 * Transform the exitcode known by the batchframework into an exitcode in the
	 * format of the calling environment.
	 * @param exitCode	The exitcode which is used internally by the batch framework.
	 * @return			The corresponding exitcode as known by the calling environment.
	 */
	public int getExitCode(String exitCode);
	
}
