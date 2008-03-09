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
package org.springframework.batch.core.tasklet.support;

import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator;
import org.springframework.batch.repeat.ExitStatus;

/**
 * A {@link Tasklet} that wraps a method in a POJO. By default the
 * {@link ExitStatus} is determined by comparing the return value from the POJO
 * with null. The POJO method is usually going to have no arguments, but a
 * static argument or array of arguments can be used by setting the arguments
 * property.
 * 
 * @see AbstractMethodInvokingDelegator
 * 
 * @author Dave Syer
 * 
 */
public class TaskletAdapter extends AbstractMethodInvokingDelegator implements Tasklet {

	/**
	 * Delegate execution to the target object and translate the return value to
	 * an {@link ExitStatus} by invoking a method in the delegate POJO. N.B. the
	 * delegate method should not be void, otherwise there is no way to
	 * determine when the result indicates a finished job.
	 * 
	 * @see org.springframework.batch.core.tasklet.Tasklet#execute()
	 */
	public ExitStatus execute() throws Exception {
		return mapResult(invokeDelegateMethod());
	}

	/**
	 * If the result is an {@link ExitStatus} already just return that,
	 * otherwise return {@link ExitStatus#FINISHED} if the result is null and
	 * {@link ExitStatus#CONTINUABLE} if not.
	 * @param result the value returned by the delegate method
	 * @return an {@link ExitStatus} consistent with the result
	 */
	protected ExitStatus mapResult(Object result) {
		if (result instanceof ExitStatus) {
			return (ExitStatus) result;
		}
		if (result == null) {
			return ExitStatus.FINISHED;
		}
		return ExitStatus.CONTINUABLE;
	}

}
