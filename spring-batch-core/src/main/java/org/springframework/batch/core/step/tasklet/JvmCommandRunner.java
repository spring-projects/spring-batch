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
 * Implementation of the {@link CommandRunner} interface that calls the standard
 * {@link Runtime#exec} method. It should be noted that there is no unit tests for this
 * class, since there is only one line of actual code, that would only be testable by
 * mocking {@link Runtime}.
 *
 * @author Stefano Cordio
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class JvmCommandRunner implements CommandRunner {

	/**
	 * Delegate call to {@link Runtime#exec} with the arguments provided.
	 *
	 * @see CommandRunner#exec(String[], String[], File)
	 */
	@Override
	public Process exec(String command[], String[] envp, File dir) throws IOException {
		return Runtime.getRuntime().exec(command, envp, dir);
	}

}
