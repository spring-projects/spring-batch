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
package org.springframework.batch.core.launch.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.explore.BatchMetaDataExplorer;
import org.springframework.batch.core.job.JobSupport;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.StepSupport;
import org.springframework.batch.support.PropertiesConverter;

/**
 * @author Dave Syer
 * 
 */
public class SimpleJobOperatorTests {

	private SimpleJobOperator jobOperator;

	protected Job job;

	private BatchMetaDataExplorer batchMetaDataExplorer;

	private JobParameters jobParameters;

	/**
	 * @throws Exception
	 * 
	 */
	@Before
	public void setUp() throws Exception {

		job = new JobSupport("foo") {
			@Override
			public JobParametersIncrementer getJobParametersIncrementer() {
				return new JobParametersIncrementer() {
					public JobParameters getNext(JobParameters parameters) {
						return jobParameters;
					}
				};
			}
		};

		jobOperator = new SimpleJobOperator();

		jobOperator.setJobRegistry(new MapJobRegistry() {
			public Job getJob(String name) throws NoSuchJobException {
				if (name.equals("foo")) {
					return job;
				}
				throw new NoSuchJobException("foo");
			}
			@Override
			public Collection<String> getJobNames() {
				return Arrays.asList(new String[] {"foo", "bar"});
			}
		});

		jobOperator.setJobLauncher(new JobLauncher() {
			public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
					JobRestartException, JobInstanceAlreadyCompleteException {
				return new JobExecution(new JobInstance(123L, jobParameters, job.getName()), 999L);
			}
		});

		batchMetaDataExplorer = EasyMock.createNiceMock(BatchMetaDataExplorer.class);

		jobOperator.setBatchMetaDataExplorer(batchMetaDataExplorer);

		jobOperator.setJobParametersConverter(new DefaultJobParametersConverter() {
			@Override
			public JobParameters getJobParameters(Properties props) {
				assertTrue("Wrong properties", props.containsKey("a"));
				return jobParameters;
			}

			@Override
			public Properties getProperties(JobParameters params) {
				return PropertiesConverter.stringToProperties("a=b");
			}
		});

		jobOperator.afterPropertiesSet();

	}

	@Test
	public void testMandatoryProperties() throws Exception {
		jobOperator = new SimpleJobOperator();
		try {
			jobOperator.afterPropertiesSet();
			fail("Expected IllegalArgumentException");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
	}

	@Test
	public void testStop() throws Exception {
		try {
			jobOperator.stop(123L);
			fail("Expected UnsupportedOperationException");
		}
		catch (UnsupportedOperationException e) {
			// expected
		}
	}

	/**
	 * Test method for
	 * {@link org.springframework.batch.core.launch.support.SimpleJobOperator#startNextInstance(java.lang.String)}
	 * .
	 * @throws Exception
	 */
	@Test
	public void testStartNextInstanceSunnyDay() throws Exception {
		final JobParameters jobParameters = new JobParameters();
		batchMetaDataExplorer.getLastJobInstances("foo", 1);
		EasyMock.expectLastCall().andReturn(Collections.singletonList(new JobInstance(321L, jobParameters, "foo")));
		EasyMock.replay(batchMetaDataExplorer);
		Long value = jobOperator.startNextInstance("foo");
		assertEquals(999, value.longValue());
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testStartNewInstanceSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.isJobInstanceExists("foo", jobParameters);
		EasyMock.expectLastCall().andReturn(false);
		EasyMock.replay(batchMetaDataExplorer);
		Long value = jobOperator.start("foo", "a=b");
		assertEquals(999, value.longValue());
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testStartNewInstanceAlreadyExists() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.isJobInstanceExists("foo", jobParameters);
		EasyMock.expectLastCall().andReturn(true);
		EasyMock.replay(batchMetaDataExplorer);
		try {
			jobOperator.start("foo", "a=b");
			fail("Expected JobInstanceAlreadyExistsException");
		}
		catch (JobInstanceAlreadyExistsException e) {
			// expected
		}
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testResumeSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.getJobExecution(111L);
		EasyMock.expectLastCall()
				.andReturn(new JobExecution(new JobInstance(123L, jobParameters, job.getName()), 111L));
		EasyMock.replay(batchMetaDataExplorer);
		Long value = jobOperator.resume(111L);
		assertEquals(999, value.longValue());
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testGetSummarySunnyDay() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.getJobExecution(111L);
		JobExecution jobExecution = new JobExecution(new JobInstance(123L, jobParameters, job.getName()), 111L);
		EasyMock.expectLastCall().andReturn(jobExecution);
		EasyMock.replay(batchMetaDataExplorer);
		String value = jobOperator.getSummary(111L);
		assertEquals(jobExecution.toString(), value);
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testGetStepExecutionSummariesSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.getJobExecution(111L);
		JobExecution jobExecution = new JobExecution(new JobInstance(123L, jobParameters, job.getName()), 111L);
		jobExecution.createStepExecution(new StepSupport("step1"));
		jobExecution.createStepExecution(new StepSupport("step2"));
		jobExecution.getStepExecutions().iterator().next().setId(21L);
		EasyMock.expectLastCall().andReturn(jobExecution);
		EasyMock.replay(batchMetaDataExplorer);
		Map<Long, String> value = jobOperator.getStepExecutionSummaries(111L);
		assertEquals(2, value.size());
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testFindRunningExecutionsSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.findRunningJobExecutions("foo");
		JobExecution jobExecution = new JobExecution(new JobInstance(123L, jobParameters, job.getName()), 111L);
		EasyMock.expectLastCall().andReturn(Collections.singleton(jobExecution));
		EasyMock.replay(batchMetaDataExplorer);
		Set<Long> value = jobOperator.getRunningExecutions("foo");
		assertEquals(111L, value.iterator().next().longValue());
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testGetJobParametersSunnyDay() throws Exception {
		final JobParameters jobParameters = new JobParameters();
		batchMetaDataExplorer.getJobExecution(111L);
		EasyMock.expectLastCall()
				.andReturn(new JobExecution(new JobInstance(123L, jobParameters, job.getName()), 111L));
		EasyMock.replay(batchMetaDataExplorer);
		String value = jobOperator.getParameters(111L);
		assertEquals("a=b", value);
		EasyMock.verify(batchMetaDataExplorer);
	}

	@Test
	public void testGetLastInstancesSunnyDay() throws Exception {
		jobParameters = new JobParameters();
		batchMetaDataExplorer.getLastJobInstances("foo",2);
		JobInstance jobInstance = new JobInstance(123L, jobParameters, job.getName());
		EasyMock.expectLastCall().andReturn(Collections.singletonList(jobInstance));
		EasyMock.replay(batchMetaDataExplorer);
		List<Long> value = jobOperator.getLastInstances("foo",2);
		assertEquals(123L, value.get(0).longValue());
		EasyMock.verify(batchMetaDataExplorer);
	}
	
	@Test
	public void testGetJobNames() throws Exception {
		Set<String> names = jobOperator.getJobNames();
		assertEquals(2, names.size());
		assertTrue("Wrong names: "+names, names.contains("foo"));
	}

	@Test
	public void testGetExecutionsSunnyDay() throws Exception {
		JobInstance jobInstance = new JobInstance(123L, jobParameters, job.getName());
		batchMetaDataExplorer.getJobInstance(123L);
		EasyMock.expectLastCall().andReturn(jobInstance);
		JobExecution jobExecution = new JobExecution(jobInstance, 111L);
		batchMetaDataExplorer.findJobExecutions(jobInstance);
		EasyMock.expectLastCall().andReturn(Collections.singletonList(jobExecution));
		EasyMock.replay(batchMetaDataExplorer);
		List<Long> value = jobOperator.getExecutions(123L);
		assertEquals(111L, value.iterator().next().longValue());
		EasyMock.verify(batchMetaDataExplorer);
	}

}
