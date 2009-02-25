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
package org.springframework.batch.sample.common;

import org.apache.commons.io.FilenameUtils;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;

/**
 * @author Dave Syer
 * 
 */
public class OutputFileListener {

	private String outputKeyName = "outputFile";

	private String inputKeyName = "fileName";

	public void setOutputKeyName(String outputKeyName) {
		this.outputKeyName = outputKeyName;
	}

	public void setInputKeyName(String inputKeyName) {
		this.inputKeyName = inputKeyName;
	}

	@BeforeStep
	public void CreateOutputNameFromInput(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		if (executionContext.containsKey(inputKeyName) && !executionContext.containsKey(outputKeyName)) {
			String inputName = executionContext.getString(inputKeyName);
			executionContext.putString(outputKeyName, "file:./target/output/" + FilenameUtils.getBaseName(inputName) + ".csv");
		}
	}

}
