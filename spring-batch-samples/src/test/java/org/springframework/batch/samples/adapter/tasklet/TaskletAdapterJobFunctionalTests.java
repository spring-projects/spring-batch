/*
 * Copyright 2006-2025 the original author or authors.
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

package org.springframework.batch.samples.adapter.tasklet;

import org.junit.jupiter.api.Test;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.test.JobOperatorTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringJUnitConfig(locations = { "/org/springframework/batch/samples/adapter/tasklet/taskletJob.xml",
		"/simple-job-launcher-context.xml" })
class TaskletAdapterJobFunctionalTests {

	@Autowired
	private JobOperatorTestUtils jobOperatorTestUtils;

	@Test
	void testLaunchJob() throws Exception {
		JobExecution jobExecution = jobOperatorTestUtils
			.startJob(new JobParametersBuilder().addString("value", "foo").toJobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals("yes", jobExecution.getExecutionContext().getString("done"));
	}

	static class TestBean {

		private String value;

		public void setValue(String value) {
			this.value = value;
		}

		public void execute(String strValue, Integer integerValue, double doubleValue) {
			assertEquals("foo", value);
			assertEquals("foo2", strValue);
			assertEquals(3, integerValue.intValue());
			assertEquals(3.14, doubleValue, 0.01);
		}

	}

}
