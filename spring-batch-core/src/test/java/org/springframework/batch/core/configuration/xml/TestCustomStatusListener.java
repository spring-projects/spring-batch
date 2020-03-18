/*
 * Copyright 2009-2019 the original author or authors.
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

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;

public class TestCustomStatusListener extends AbstractTestComponent implements StepExecutionListener {

	@Nullable
	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		return new ExitStatus("FOO").and(stepExecution.getExitStatus());
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		executed = true;
	}

}
