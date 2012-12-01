/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.step.tasklet.Tasklet;

/**
 * @author Dave Syer
 * 
 */
public class TaskletStepBuilder extends AbstractTaskletStepBuilder<TaskletStepBuilder> {

	private Tasklet tasklet;

	public TaskletStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	public TaskletStepBuilder tasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
		return this;
	}
	
	@Override
	protected Tasklet createTasklet() {
		return tasklet;
	}

}
