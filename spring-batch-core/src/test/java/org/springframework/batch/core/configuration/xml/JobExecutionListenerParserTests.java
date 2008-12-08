/*
 * Copyright 2002-2008 the original author or authors.
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

import static junit.framework.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.annotation.AfterJob;
import org.springframework.batch.core.annotation.BeforeJob;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Lucas Ward
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JobExecutionListenerParserTests {

	public static boolean beforeCalled = false;
	public static boolean afterCalled = false;
	
	@Autowired
	Job job;
	
	@Autowired
	JobRepository jobRepository;
	
	@Test
	public void testListeners() throws Exception{
		JobExecution jobExecution = jobRepository.createJobExecution("testJob", new JobParametersBuilder().addLong("now", 
				System.currentTimeMillis()).toJobParameters());
		job.execute(jobExecution);
		assertTrue(beforeCalled);
		assertTrue(afterCalled);
	}
	
	public static class TestComponent{
		
		@BeforeJob
		public void before(JobExecution jobExecution){
			beforeCalled = true;
		}
		
		@AfterJob 
		public void after(){
			afterCalled = true;
		}
	}
	
}
