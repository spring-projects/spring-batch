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
package org.springframework.batch.core.executor;

import org.springframework.batch.core.domain.Step;

/**
 * Factory interface for creating or locating {@link StepExecutor} instances.
 * Because step execution parameters and policies can vary from step to step, a
 * {@link StepExecutor} should be created by the caller using a
 * {@link StepExecutorFactory}. The factory is responsible for ensuring that
 * the returned instance is appropriate for the step supplied. If the
 * {@link StepExecutor} instance is stateful (which is normal) the factory
 * should return a different instance for each call.
 * 
 * @author Dave Syer
 * 
 */
public interface StepExecutorFactory {

	/**
	 * Use the step given to create or locate a suitable
	 * {@link StepExecutor}.
	 * 
	 * @param step a {@link Step} instance.
	 * @return a {@link StepExecutor} that can be used to execute a step with
	 * the given step
	 */
	StepExecutor getExecutor(Step step);

}
