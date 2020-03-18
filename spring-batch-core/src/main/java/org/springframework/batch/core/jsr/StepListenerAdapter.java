/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.jsr;

import javax.batch.api.listener.StepListener;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Wrapper class to adapt the {@link StepListener} to
 * a {@link StepExecutionListener}.
 * 
 * @author Michael Minella
 * @since 3.0
 */
public class StepListenerAdapter implements StepExecutionListener {

	private final StepListener delegate;

	/**
	 * @param delegate instance of {@link StepListener}.
	 */
	public StepListenerAdapter(StepListener delegate) {
		Assert.notNull(delegate, "A listener is required");
		this.delegate = delegate;
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		try {
			delegate.beforeStep();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}

	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		try {
			delegate.afterStep();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}

		return stepExecution.getExitStatus();
	}
}
