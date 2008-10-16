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
package org.springframework.batch.core.scope;

import org.springframework.batch.core.Step;
import org.springframework.batch.repeat.ExitStatus;
import org.springframework.batch.repeat.RepeatCallback;
import org.springframework.batch.repeat.RepeatContext;

/**
 * Convenient base class for clients who need to do something in a repeat
 * callback inside a {@link Step}.
 * 
 * @author Dave Syer
 * 
 */
public abstract class StepContextRepeatCallback implements RepeatCallback {

	private final StepContext stepContext;

	/**
	 * @param stepContext
	 */
	public StepContextRepeatCallback(StepContext stepContext) {
		this.stepContext = stepContext;
	}

	/**
	 * Manage the {@link StepContext} lifecycle to ensure that the current
	 * thread has a reference to the context, even if the callback is executed
	 * in a pooled thread.
	 * 
	 * @see RepeatCallback#doInIteration(RepeatContext)
	 */
	public ExitStatus doInIteration(RepeatContext context) throws Exception {
		StepSynchronizationManager.register(stepContext);
		try {
			return doInStepContext(context, stepContext);
		}
		finally {
			StepSynchronizationManager.close();
		}
	}

	/**
	 * @param context
	 * @param stepContext
	 * @return the exit status from the execution
	 * @throws Exception
	 */
	public abstract ExitStatus doInStepContext(RepeatContext context, StepContext stepContext) throws Exception;

}
