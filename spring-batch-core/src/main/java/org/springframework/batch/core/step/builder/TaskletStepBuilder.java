/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.step.tasklet.Tasklet;

/**
 * Builder for tasklet step based on a custom tasklet (not item oriented).
 * 
 * @author Dave Syer
 * 
 * @since 2.2
 */
public class TaskletStepBuilder extends AbstractTaskletStepBuilder<TaskletStepBuilder> {

	private Tasklet tasklet;

	/**
	 * Create a new builder initialized with any properties in the parent.  The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	public TaskletStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * @param tasklet the tasklet to use
	 * @return this for fluent chaining
	 */
	public TaskletStepBuilder tasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
		return this;
	}
	
	@Override
	protected Tasklet createTasklet() {
		return tasklet;
	}

}
