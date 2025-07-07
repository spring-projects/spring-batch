/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.batch.samples.validation;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.support.ListItemWriter;
import org.springframework.batch.samples.validation.domain.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Mahmoud Ben Hassine
 * @author Glenn Renfro
 */
@SpringJUnitConfig(classes = { ValidationSampleConfiguration.class })
class ValidationSampleFunctionalTests {

	@Autowired
	private Job job;

	@Autowired
	private JobLauncher jobLauncher;

	@Autowired
	private ListItemWriter<Person> listItemWriter;

	@Test
	void testItemValidation() throws Exception {
		// given
		JobParameters jobParameters = new JobParameters();

		// when
		JobExecution jobExecution = this.jobLauncher.run(this.job, jobParameters);

		// then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), jobExecution.getExitStatus().getExitCode());
		List<? extends Person> writtenItems = this.listItemWriter.getWrittenItems();
		assertEquals(1, writtenItems.size());
		assertEquals("foo", writtenItems.get(0).getName());
	}

}
