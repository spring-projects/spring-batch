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

package org.springframework.batch.execution.step;

import org.springframework.batch.core.configuration.StepConfiguration;
import org.springframework.batch.core.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatOperations;

/**
 * {@link StepConfiguration} implementation that allows full configuration of
 * the {@link RepeatOperations} that will be used in the chunk (inner loop).
 * 
 * @author Lucas Ward
 * @author Dave Syer
 * 
 */
public class ChunkOperationsStepConfiguration extends AbstractStepConfiguration implements RepeatOperationsHolder {

	// default StepExecutor is null
	private RepeatOperations chunkOperations;

	public ChunkOperationsStepConfiguration() {
		super();
	}

	public ChunkOperationsStepConfiguration(RepeatOperations repeatOperations) {
		this();
		this.chunkOperations = repeatOperations;
	}

	public ChunkOperationsStepConfiguration(Tasklet module) {
		this();
		setTasklet(module);
	}

	/**
	 * Public accessor for the chunkOperations property.
	 * 
	 * @return the executor
	 */
	public RepeatOperations getChunkOperations() {
		return chunkOperations;
	}

	/**
	 * Public setter for the chunkOperations.
	 * 
	 * @param chunkOperations the repeatOperations to set
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

}
