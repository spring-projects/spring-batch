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
package org.springframework.batch.execution.step.simple;

import org.springframework.batch.core.domain.Step;
import org.springframework.batch.core.domain.StepExecutor;
import org.springframework.batch.repeat.RepeatOperations;

/**
 * Marker interface for indicating that a {@link RepeatOperations} instance is
 * available for the inner loop (chunk operations) and outer loop (step
 * operations) in a {@link StepExecutor}. The inner loop is normally going to
 * be in-process and thread-bound so it makes sense for
 * {@link Step} implementations to be able to override the
 * strategies that control that loop.
 * 
 * @author Dave Syer
 * 
 */
public interface RepeatOperationsHolder {

	/**
	 * Principal method in the {@link RepeatOperationsHolder} interface.
	 * 
	 * @return a {@link RepeatOperations} which can be used to iterate over an
	 *         inner loop (chunk).
	 */
	RepeatOperations getChunkOperations();

	/**
	 * Additional method in the {@link RepeatOperationsHolder} interface.
	 * 
	 * @return a {@link RepeatOperations} which can be used to iterate over an
	 *         outer loop (step).
	 */
	RepeatOperations getStepOperations();
}
