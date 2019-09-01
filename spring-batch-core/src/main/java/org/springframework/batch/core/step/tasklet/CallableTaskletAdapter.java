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

import java.util.concurrent.Callable;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Adapts a {@link Callable}&lt;{@link RepeatStatus}&gt; to the {@link Tasklet}
 * interface.
 *
 * @author Dave Syer
 *
 */
public class CallableTaskletAdapter implements Tasklet, InitializingBean {

	private Callable<RepeatStatus> callable;

	/**
	 * Public setter for the {@link Callable}.
	 * @param callable the {@link Callable} to set
	 */
	public void setCallable(Callable<RepeatStatus> callable) {
		this.callable = callable;
	}

	/**
	 * Assert that the callable is set.
	 *
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(callable, "A Callable is required");
	}

	/**
	 * Execute the provided Callable and return its {@link RepeatStatus}. Ignores
	 * the {@link StepContribution} and the attributes.
	 * @see Tasklet#execute(StepContribution, ChunkContext)
	 */
	@Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		return callable.call();
	}

}
