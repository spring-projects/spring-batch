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
package org.springframework.batch.sample.quartz;

import static org.mockito.Mockito.mock;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.impl.triggers.SimpleTriggerImpl;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.lang.Nullable;

/**
 * @author Dave Syer
 * 
 */
public class JobLauncherDetailsTests {
	private JobLauncherDetails details = new JobLauncherDetails();
	private TriggerFiredBundle firedBundle;
	private List<Serializable> list = new ArrayList<>();
	
	@Before
	public void setUp() throws Exception {
		details.setJobLauncher(new JobLauncher() {
			@Override
			public JobExecution run(org.springframework.batch.core.Job job, JobParameters jobParameters)
					throws JobExecutionAlreadyRunningException, JobRestartException {
				list.add(jobParameters);
				return null;
			}
		});

		details.setJobLocator(new JobLocator() {
			@Override
			public org.springframework.batch.core.Job getJob(@Nullable String name) throws NoSuchJobException {
				list.add(name);
				return new StubJob("foo");
			}
		});
	}

	private JobExecutionContext createContext(JobDetail jobDetail) {
		firedBundle = new TriggerFiredBundle(jobDetail, new SimpleTriggerImpl(), null, false, new Date(), new Date(), new Date(), new Date());
		return new StubJobExecutionContext();
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.quartz.JobLauncherDetails#executeInternal(org.quartz.JobExecutionContext)}.
	 */
	@Test
	public void testExecuteWithNoJobParameters() {
		JobDetail jobDetail = new JobDetailImpl();
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		JobParameters parameters = (JobParameters) list.get(1);
		assertEquals(0, parameters.getParameters().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.quartz.JobLauncherDetails#executeInternal(org.quartz.JobExecutionContext)}.
	 */
	@Test
	public void testExecuteWithJobName() {
		JobDetail jobDetail = new JobDetailImpl();
		jobDetail.getJobDataMap().put(JobLauncherDetails.JOB_NAME, "FOO");
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		assertEquals("FOO", list.get(0));
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.quartz.JobLauncherDetails#executeInternal(org.quartz.JobExecutionContext)}.
	 */
	@Test
	public void testExecuteWithSomeJobParameters() {
		JobDetail jobDetail = new JobDetailImpl();
		jobDetail.getJobDataMap().put("foo", "bar");
		JobExecutionContext context = createContext(jobDetail);
		details.executeInternal(context);
		assertEquals(2, list.size());
		JobParameters parameters = (JobParameters) list.get(1);
		assertEquals(1, parameters.getParameters().size());
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.quartz.JobLauncherDetails#executeInternal(org.quartz.JobExecutionContext)}.
	 */
	@Test
	public void testExecuteWithJobNameAndParameters() {
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

	/**
	 * Test method for
	 * {@link org.springframework.batch.sample.quartz.JobLauncherDetails#executeInternal(org.quartz.JobExecutionContext)}.
	 */
	@Test
	public void testExecuteWithJobNameAndComplexParameters() {
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

	@SuppressWarnings("serial")
	private final class StubJobExecutionContext extends JobExecutionContextImpl {
		private StubJobExecutionContext() {
			super(mock(Scheduler.class), firedBundle, mock(Job.class));
		}
	}
	
	private static class StubJob implements org.springframework.batch.core.Job {
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
