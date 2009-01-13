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

import java.util.Map;

import org.springframework.batch.core.ExitStatus;
import org.springframework.util.Assert;

/**
 * Maps exit codes to {@link org.springframework.batch.core.ExitStatus} 
 * according to injected map. The injected map is required to contain a value 
 * for 'else' key, this value will be returned if the injected map 
 * does not contain value for the exit code returned by the system process.
 * 
 * @author Robert Kasanicky
 */
public class ConfigurableSystemProcessExitCodeMapper implements SystemProcessExitCodeMapper {
	
	public static final String ELSE_KEY = "else";
	
	private Map<Object, ExitStatus> mappings;

	public ExitStatus getExitStatus(int exitCode) {
		ExitStatus exitStatus = mappings.get(exitCode);
		if (exitStatus != null) {
			return exitStatus;
		} else {
			return mappings.get(ELSE_KEY);
		}
	}

	/**
	 * @param mappings <code>Integer</code> exit code keys to 
	 * {@link org.springframework.batch.core.ExitStatus} values.
	 */
	public void setMappings(Map<Object, ExitStatus> mappings) {
		Assert.notNull(mappings.get(ELSE_KEY));
		this.mappings = mappings;
	}

}
