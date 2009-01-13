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

package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;

/**
 * Maps the exit code of a system process to ExitStatus value
 * returned by a system command. Designed for use with the
 * {@link SystemCommandTasklet}.
 * 
 * @author Robert Kasanicky
 */
public interface SystemProcessExitCodeMapper {
	
	/** 
	 * @param exitCode exit code returned by the system process
	 * @return ExitStatus appropriate for the <code>systemExitCode</code> parameter value
	 */
	ExitStatus getExitStatus(int exitCode);
}
