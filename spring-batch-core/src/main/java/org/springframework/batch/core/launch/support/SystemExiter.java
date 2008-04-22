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
package org.springframework.batch.core.launch.support;

/**
 * Interface for exiting the JVM.  This abstraction is only
 * useful in order to allow classes that make System.exit calls
 * to be testable, since calling System.exit during a unit 
 * test would cause the entire jvm to finish.
 * 
 * @author Lucas Ward
 *
 */
public interface SystemExiter {

	/**
	 * Terminate the currently running Java Virtual Machine.
	 * 
	 * @param status exit status.
	 * @throws SecurityException
	 * 		if a security manager exists and its <code>checkExit</code>
     *        	method doesn't allow exit with the specified status.
     * @see System#exit(int)
	 */
	void exit(int status);
}
