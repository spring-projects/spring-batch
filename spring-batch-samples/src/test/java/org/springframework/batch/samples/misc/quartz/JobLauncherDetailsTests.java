/*
 * Copyright 2006-2023 the original author or authors.
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
package org.springframework.batch.samples.misc.quartz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersIncrementer;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.job.Job;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Dave Syer
 * @author Glenn Renfro
 * @author Mahmoud Ben Hassine
 * 
 */
class JobLauncherDetailsTests {

	private final JobLauncherDetails details = new JobLauncherDetails();

	private TriggerFiredBundle firedBundle;

	private final List<Serializable> list = new ArrayList<>();

	@BeforeEach
	public void setUp() throws Exception {
		details.setJobLauncher((job, jobParameters) -> {
			list.add(jobParameters);
			return null;
		});

		details.setJobLocator(name -> {
			list.add(name);
			return new StubJob("foo");
		});
	}

	private JobExecutionContext createContext(JobDetail jobDetail) {
		firedBundle = new TriggerFiredBundle(jobDetail, new SimpleTriggerImpl(), null, false, new Date(), new Date(),
				new Date(), new Date());
		return new StubJobExecutionContext();
	}

	@Test
	void testExecuteWithNoJobParameters() {
		JobDetail jobDetail = new JobDetailImpl();
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		JobParameters parameters = (JobParameters) list.get(1);
		assertEquals(0, parameters.getParameters().size());
	}

	@Test
	void testExecuteWithJobName() {
		JobDetail jobDetail = new JobDetailImpl();
		jobDetail.getJobDataMap().put(JobLauncherDetails.JOB_NAME, "FOO");
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		assertEquals("FOO", list.get(0));
	}

	@Test
	void testExecuteWithSomeJobParameters() {
		JobDetail jobDetail = new JobDetailImpl();
		jobDetail.getJobDataMap().put("foo", "bar");
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		JobParameters parameters = (JobParameters) list.get(1);
		assertEquals(1, parameters.getParameters().size());
	}

	@Test
	void testExecuteWithJobNameAndParameters() {
		JobDetail jobDetail = new JobDetailImpl();
		jobDetail.getJobDataMap().put(JobLauncherDetails.JOB_NAME, "FOO");
		jobDetail.getJobDataMap().put("foo", "bar");
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		assertEquals("FOO", list.get(0));
		JobParameters parameters = (JobParameters) list.get(1);
		assertEquals(1, parameters.getParameters().size());
	}

	@Test
	void testExecuteWithJobNameAndComplexParameters() {
		JobDetail jobDetail = new JobDetailImpl();
		jobDetail.getJobDataMap().put(JobLauncherDetails.JOB_NAME, "FOO");
		jobDetail.getJobDataMap().put("foo", this);
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		assertEquals("FOO", list.get(0));
		JobParameters parameters = (JobParameters) list.get(1);
		// Silently ignore parameters that are not simple types
		assertEquals(0, parameters.getParameters().size());
	}

	private final class StubJobExecutionContext extends JobExecutionContextImpl {

		private StubJobExecutionContext() {
			super(mock(), firedBundle, mock());
		}

	}

	private static class StubJob implements Job {

		private final String name;

		public StubJob(String name) {
			this.name = name;
		}

		@Override
		public void execute(JobExecution execution) {
		}

		@Nullable
		@Override
		public JobParametersIncrementer getJobParametersIncrementer() {
			return null;
		}

		@Override
		public JobParametersValidator getJobParametersValidator() {
			return null;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isRestartable() {
			return false;
		}

	}

}
