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
package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Dave Syer
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class SplitJobParserTests {
	
	@Autowired
	@Qualifier("job")
	private Job job;

	@Autowired
	private JobRepository jobRepository;
	
	@Test
	public void testSplitJob() throws Exception {
		assertNotNull(job);
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), new JobParameters());
		job.execute(jobExecution);
		assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
		assertEquals(4, jobExecution.getStepExecutions().size());
		ArrayList<String> names = new ArrayList<String>(((StepLocator)job).getStepNames());
		Collections.sort(names);
		assertEquals("[s1, s2, s3, s4]", names.toString());
	}

}
