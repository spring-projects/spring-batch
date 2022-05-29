/*
 * Copyright 2006-2021 the original author or authors.
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
package org.springframework.batch.core.listener;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * @deprecated as of 5.0, in favor of the default methods on the
 * {@link StepExecutionListener}
 */
@Deprecated
public class StepExecutionListenerSupport implements StepExecutionListener {

	/*
	 * (non-Javadoc)
	 *
	 * @see org.springframework.batch.core.domain.StepListener#afterStep(StepExecution
	 * stepExecution)
	 */
	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.batch.core.domain.StepListener#open(org.springframework.batch.
	 * item.ExecutionContext)
	 */
	@Override
	public void beforeStep(StepExecution stepExecution) {
	}

}
