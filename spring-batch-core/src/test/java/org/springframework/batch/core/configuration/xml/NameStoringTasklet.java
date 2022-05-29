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
package org.springframework.batch.core.configuration.xml;

import java.util.List;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;

/**
 * This class will store the step name when it is executed.
 *
 * @author Dan Garrette
 * @author Mahmoud Ben Hassine
 * @since 2.0
 */
public class NameStoringTasklet implements StepExecutionListener, Tasklet {

	private String stepName = null;

	private List<String> stepNamesList = null;

	@Override
	public void beforeStep(StepExecution stepExecution) {
		stepName = stepExecution.getStepName();
	}

	@Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		if (stepNamesList != null) {
			stepNamesList.add(stepName);
		}
		return RepeatStatus.FINISHED;
	}

	public void setStepNamesList(List<String> stepNamesList) {
		this.stepNamesList = stepNamesList;
	}

}
