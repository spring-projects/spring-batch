/*
 * Copyright 2006-2018 the original author or authors.
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

package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/taskletJob.xml",
		"/job-runner-context.xml" })
public class TaskletJobFunctionalTests {

	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void testLaunchJob() throws Exception {
		JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParametersBuilder().addString("value", "foo")
				.toJobParameters());
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals("yes", jobExecution.getExecutionContext().getString("done"));
	}

	public static class TestBean {
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
	
	public static class Task {
		
		public boolean doWork(ChunkContext chunkContext) {
			chunkContext.
					getStepContext().
					getStepExecution().
					getJobExecution().
					getExecutionContext().put("done", "yes");
			return true;
		}
		
	}

}
