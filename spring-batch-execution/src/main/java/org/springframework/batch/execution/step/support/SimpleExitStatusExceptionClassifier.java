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
package org.springframework.batch.execution.step.support;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.springframework.batch.core.domain.JobInterruptedException;
import org.springframework.batch.core.runtime.ExitStatusExceptionClassifier;
import org.springframework.batch.repeat.ExitStatus;

/**
 * <p>
 * Simple implementation of {@link ExitStatusExceptionClassifier} that returns
 * basic String exit codes, and defaults to the class name of the throwable for
 * the message. Most users will want to write their own implementation that
 * creates more specific exit codes for different exception types.
 * </p>
 * 
 * @author Lucas Ward
 * 
 */
public class SimpleExitStatusExceptionClassifier implements
		ExitStatusExceptionClassifier {

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.executor.ExitCodeExceptionClassifier#classifyForExitCode(java.lang.Throwable)
	 */
	public ExitStatus classifyForExitCode(Throwable throwable) {
		return (ExitStatus) classify(throwable);
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.common.ExceptionClassifier#classify(java.lang.Throwable)
	 */
	public Object classify(Throwable throwable) {

		ExitStatus exitStatus = ExitStatus.FAILED;

		if (throwable instanceof JobInterruptedException) {
			exitStatus = new ExitStatus(false, JOB_INTERRUPTED,
					JobInterruptedException.class.getName());
		} else {
			String message = "";
			if (throwable!=null) {
				StringWriter writer = new StringWriter();
				throwable.printStackTrace(new PrintWriter(writer));
				message = writer.toString();
			}
			exitStatus = new ExitStatus(false, FATAL_EXCEPTION, message);
		}

		return exitStatus;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.batch.common.ExceptionClassifier#getDefault()
	 */
	public Object getDefault() {
		// return without message since we don't know what the exception is
		return new ExitStatus(false, FATAL_EXCEPTION);
	}

}
