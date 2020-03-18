/*
 * Copyright 2006-2019 the original author or authors.
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

package org.springframework.batch.core.test.step;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * 
 */
public class SplitJobMapRepositoryIntegrationTests {

	private static final int MAX_COUNT = 1000;

	/** Logger */
	private final Log logger = LogFactory.getLog(getClass());

	@SuppressWarnings("resource")	
	@Test
	public void testMultithreadedSplit() throws Throwable {

		JobLauncher jobLauncher = null;
		Job job = null;
		
		ClassPathXmlApplicationContext context = null;

		for (int i = 0; i < MAX_COUNT; i++) {

			if (i % 100 == 0) {
				if (context!=null) {
					context.close();
				}
				logger.info("Starting job: " + i);
				context = new ClassPathXmlApplicationContext(getClass().getSimpleName()
						+ "-context.xml", getClass());
				jobLauncher = context.getBean("jobLauncher", JobLauncher.class);
				job = context.getBean("job", Job.class);
			}

			try {
				JobExecution execution = jobLauncher.run(job, new JobParametersBuilder().addLong("count", new Long(i))
						.toJobParameters());
				assertEquals(BatchStatus.COMPLETED, execution.getStatus());
			}
			catch (Throwable e) {
				logger.info("Failed on iteration " + i + " of " + MAX_COUNT);
				throw e;
			}

		}

	}

	public static class CountingTasklet implements Tasklet {

		private int maxCount = 10;

		private AtomicInteger count = new AtomicInteger(0);

		@Nullable
		@Override
		public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
			contribution.incrementReadCount();
			contribution.incrementWriteCount(1);
			return RepeatStatus.continueIf(count.incrementAndGet() < maxCount);
		}

	}

}
