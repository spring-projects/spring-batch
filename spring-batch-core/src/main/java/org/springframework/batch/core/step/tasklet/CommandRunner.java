/*
 * Copyright 2006-2022 the original author or authors.
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

import java.io.File;
import java.io.IOException;

/**
 * Strategy interface for executing commands. This abstraction is useful to decouple the
 * command execution from the enclosing tasklet so that implementations can be unit tested
 * in isolation.
 *
 * @author Stefano Cordio
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public interface CommandRunner {

	/**
	 * Executes the specified string command in a separate process with the specified
	 * environment and working directory.
	 * @param command a specified system command and its arguments.
	 * @param envp array of strings, each element of which has environment variable
	 * settings in the format <i>name</i>=<i>value</i>, or {@code null} if the subprocess
	 * should inherit the environment of the current process.
	 * @param dir the working directory of the subprocess, or {@code null} if the
	 * subprocess should inherit the working directory of the current process.
	 * @return A new {@link Process} object for managing the subprocess
	 * @throws SecurityException If a security manager exists and its
	 * {@link SecurityManager#checkExec checkExec} method doesn't allow creation of the
	 * subprocess
	 * @throws IOException If an I/O error occurs
	 * @throws NullPointerException If {@code command} is {@code null}, or one of the
	 * elements of {@code envp} is {@code null}
	 * @throws IllegalArgumentException If {@code command} is empty
	 *
	 * @see Runtime#exec(String, String[], File)
	 */
	Process exec(String command[], String[] envp, File dir) throws IOException;

}
