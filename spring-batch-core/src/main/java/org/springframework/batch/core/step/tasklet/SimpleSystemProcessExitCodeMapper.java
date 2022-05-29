/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.core.step.tasklet;

import org.springframework.batch.core.ExitStatus;

/**
 * Simple {@link SystemProcessExitCodeMapper} implementation that performs following
 * mapping:
 *
 * 0 -&gt; ExitStatus.FINISHED else -&gt; ExitStatus.FAILED
 *
 * @author Robert Kasanicky
 */
public class SimpleSystemProcessExitCodeMapper implements SystemProcessExitCodeMapper {

	@Override
	public ExitStatus getExitStatus(int exitCode) {
		if (exitCode == 0) {
			return ExitStatus.COMPLETED;
		}
		else {
			return ExitStatus.FAILED;
		}
	}

}
