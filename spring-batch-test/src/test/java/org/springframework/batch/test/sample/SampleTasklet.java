/*
 * Copyright 2008-2019 the original author or authors.
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
package org.springframework.batch.test.sample;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;

public class SampleTasklet implements Tasklet {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private JobExecution jobExecution;

	private int id = 0;

	public boolean jobContextEntryFound = false;

	public SampleTasklet(int id) {
		this.id = id;
	}

    @Nullable
	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		this.jdbcTemplate.update("insert into TESTS(ID, NAME) values (?, 'SampleTasklet" + id + "')", id);

		if (jobExecution != null) {
			ExecutionContext jobContext = jobExecution.getExecutionContext();
			if (jobContext.containsKey("key1")) {
				jobContextEntryFound = true;
			}
		}

		return RepeatStatus.FINISHED;
	}

	@BeforeStep
	public void storeJobExecution(StepExecution stepExecution) {
		this.jobExecution = stepExecution.getJobExecution();
	}
}
