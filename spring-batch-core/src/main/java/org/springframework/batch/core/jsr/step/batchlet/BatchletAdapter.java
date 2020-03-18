/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.jsr.step.batchlet;

import javax.batch.api.Batchlet;
import javax.batch.operations.BatchRuntimeException;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 *
 * @author Michael Minella
 * @since 3.0
 */
public class BatchletAdapter implements StoppableTasklet {

	private Batchlet batchlet;

	public BatchletAdapter(Batchlet batchlet) {
		Assert.notNull(batchlet, "A Batchlet implementation is required");
		this.batchlet = batchlet;
	}

	@Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution,
			ChunkContext chunkContext) throws Exception {
		String exitStatus;
		try {
			exitStatus = batchlet.process();
		} finally {
			chunkContext.setComplete();
		}

		if(StringUtils.hasText(exitStatus)) {
			contribution.setExitStatus(new ExitStatus(exitStatus));
		}


		return RepeatStatus.FINISHED;
	}

	@Override
	public void stop() {
		try {
			batchlet.stop();
		} catch (Exception e) {
			throw new BatchRuntimeException(e);
		}
	}
}
