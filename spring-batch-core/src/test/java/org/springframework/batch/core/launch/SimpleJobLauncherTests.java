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

package org.springframework.batch.core.launch;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

/**
 * @author Lucas Ward
 * 
 */
public class SimpleJobLauncherTests {

	private SimpleJobLauncher jobLauncher;

	private JobSupport job = new JobSupport("foo") {
		@Override
		public void execute(JobExecution execution) {
			execution.setExitStatus(ExitStatus.COMPLETED);
			return;
		}
	};

	private JobParameters jobParameters = new JobParameters();

	private JobRepository jobRepository;

	@Before
	public void setUp() throws Exception {

		jobLauncher = new SimpleJobLauncher();
		jobRepository = createMock(JobRepository.class);
		jobLauncher.setJobRepository(jobRepository);

	}

	@Test
	public void testRun() throws Exception {
		run(ExitStatus.COMPLETED);
	}

	@Test(expected = JobParametersInvalidException.class)
	public void testRunWithValidator() throws Exception {

		job.setJobParametersValidator(new DefaultJobParametersValidator(new String[] { "missing-and-required" },
				new String[0]));

		expect(jobRepository.getLastJobExecution(job.getName(), jobParameters)).andReturn(null);
		replay(jobRepository);

		jobLauncher.afterPropertiesSet();
		try {
			jobLauncher.run(job, jobParameters);
		}
		finally {
			verify(jobRepository);
		}

	}

	@Test
	public void testRunRestartableJobInstanceTwice() throws Exception {
		job = new JobSupport("foo") {
			@Override
			public boolean isRestartable() {
				return true;
			}

			@Override
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.COMPLETED);
				return;
			}
		};

		testRun();
		reset(jobRepository);
		expect(jobRepository.getLastJobExecution(job.getName(), jobParameters)).andReturn(
				new JobExecution(new JobInstance(1L, jobParameters, job.getName())));
		expect(jobRepository.createJobExecution(job.getName(), jobParameters)).andReturn(
				new JobExecution(new JobInstance(1L, jobParameters, job.getName())));
		replay(jobRepository);
		jobLauncher.run(job, jobParameters);
		verify(jobRepository);
	}

	/*
	 * Non-restartable JobInstance can be run only once - attempt to run
	 * existing non-restartable JobInstance causes error.
	 */
	@Test
	public void testRunNonRestartableJobInstanceTwice() throws Exception {
		job = new JobSupport("foo") {
			@Override
			public boolean isRestartable() {
				return false;
			}

			@Override
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.COMPLETED);
				return;
			}
		};

		testRun();
		try {
			reset(jobRepository);
			expect(jobRepository.getLastJobExecution(job.getName(), jobParameters)).andReturn(
					new JobExecution(new JobInstance(1L, jobParameters, job.getName())));
			replay(jobRepository);
			jobLauncher.run(job, jobParameters);
			fail("Expected JobRestartException");
		}
		catch (JobRestartException e) {
			// expected
		}
		verify(jobRepository);
	}

	@Test
	public void testTaskExecutor() throws Exception {
		final List<String> list = new ArrayList<String>();
		jobLauncher.setTaskExecutor(new TaskExecutor() {
			public void execute(Runnable task) {
				list.add("execute");
				task.run();
			}
		});
		testRun();
		assertEquals(1, list.size());
	}

	@Test
	public void testTaskExecutorRejects() throws Exception {

		final List<String> list = new ArrayList<String>();
		jobLauncher.setTaskExecutor(new TaskExecutor() {
			public void execute(Runnable task) {
				list.add("execute");
				throw new TaskRejectedException("Planned failure");
			}
		});

		JobExecution jobExecution = new JobExecution(null, null);

		expect(jobRepository.getLastJobExecution(job.getName(), jobParameters)).andReturn(null);
		expect(jobRepository.createJobExecution(job.getName(), jobParameters)).andReturn(jobExecution);
		jobRepository.update(jobExecution);
		expectLastCall();
		replay(jobRepository);

		jobLauncher.afterPropertiesSet();
		try {
			jobLauncher.run(job, jobParameters);
		}
		finally {
			assertEquals(BatchStatus.FAILED, jobExecution.getStatus());
			assertEquals(ExitStatus.FAILED.getExitCode(), jobExecution.getExitStatus().getExitCode());
			verify(jobRepository);
		}

		assertEquals(1, list.size());

	}

	@Test
	public void testRunWithException() throws Exception {
		job = new JobSupport() {
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.FAILED);
				throw new RuntimeException("foo");
			}
		};
		try {
			run(ExitStatus.FAILED);
			fail("Expected RuntimeException");
		}
		catch (RuntimeException e) {
			assertEquals("foo", e.getMessage());
		}
	}

	@Test
	public void testRunWithError() throws Exception {
		job = new JobSupport() {
			public void execute(JobExecution execution) {
				execution.setExitStatus(ExitStatus.FAILED);
				throw new Error("foo");
			}
		};
		try {
			run(ExitStatus.FAILED);
			fail("Expected Error");
		}
		catch (Error e) {
			assertEquals("foo", e.getMessage());
		}
	}

	@Test
	public void testInitialiseWithoutRepository() throws Exception {
		try {
			new SimpleJobLauncher().afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalStateException e) {
			// expected
			assertTrue("Message did not contain repository: " + e.getMessage(), contains(e.getMessage().toLowerCase(),
					"repository"));
		}
	}

	@Test
	public void testInitialiseWithRepository() throws Exception {
		jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.afterPropertiesSet(); // no error
	}

	private void run(ExitStatus exitStatus) throws Exception {
		JobExecution jobExecution = new JobExecution(null, null);

		expect(jobRepository.getLastJobExecution(job.getName(), jobParameters)).andReturn(null);
		expect(jobRepository.createJobExecution(job.getName(), jobParameters)).andReturn(jobExecution);
		replay(jobRepository);

		jobLauncher.afterPropertiesSet();
		try {
			jobLauncher.run(job, jobParameters);
		}
		finally {
			assertEquals(exitStatus, jobExecution.getExitStatus());
			verify(jobRepository);
		}
	}

	private boolean contains(String str, String searchStr) {
		return str.indexOf(searchStr) != -1;
	}
}