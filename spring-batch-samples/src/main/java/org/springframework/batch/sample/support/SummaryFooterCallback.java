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

package org.springframework.batch.sample.support;

import java.io.IOException;
import java.io.Writer;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.file.FlatFileFooterCallback;

/**
 * Writes summary info in the footer of a file.
 */
public class SummaryFooterCallback implements StepExecutionListener, FlatFileFooterCallback {

	private StepExecution stepExecution;

	@Override
	public void writeFooter(Writer writer) throws IOException {
		writer.write("footer - number of items written: " + stepExecution.getWriteCount());
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

}
