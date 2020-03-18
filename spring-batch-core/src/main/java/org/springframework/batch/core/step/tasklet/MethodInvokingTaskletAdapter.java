/*
 * Copyright 2006-2019 the original author or authors.
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
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.item.adapter.AbstractMethodInvokingDelegator;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

/**
 * A {@link Tasklet} that wraps a method in a POJO. By default the return
 * value is {@link ExitStatus#COMPLETED} unless the delegate POJO itself returns
 * an {@link ExitStatus}. The POJO method is usually going to have no arguments,
 * but a static argument or array of arguments can be used by setting the
 * arguments property.
 *
 * @see AbstractMethodInvokingDelegator
 *
 * @author Dave Syer
 * @author Mahmoud Ben Hassine
 *
 */
public class MethodInvokingTaskletAdapter extends AbstractMethodInvokingDelegator<Object> implements Tasklet {

	/**
	 * Delegate execution to the target object and translate the return value to
	 * an {@link ExitStatus} by invoking a method in the delegate POJO. Ignores
	 * the {@link StepContribution} and the attributes.
	 *
	 * @see Tasklet#execute(StepContribution, ChunkContext)
	 */
	@Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		if (getArguments() == null) {
			setArguments(new Object[]{contribution, chunkContext});
		}
		contribution.setExitStatus(mapResult(invokeDelegateMethod()));
		return RepeatStatus.FINISHED;
	}

	/**
	 * If the result is an {@link ExitStatus} already just return that,
	 * otherwise return {@link ExitStatus#COMPLETED}.
	 *
	 * @param result the value returned by the delegate method
	 * @return an {@link ExitStatus} consistent with the result
	 */
	protected ExitStatus mapResult(Object result) {
		if (result instanceof ExitStatus) {
			return (ExitStatus) result;
		}
		return ExitStatus.COMPLETED;
	}

}
