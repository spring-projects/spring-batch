package org.springframework.batch.core.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import javax.annotation.Resource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class TaskletStepAllowStartIfCompleteTest {

	@Autowired
	Job job;

	@Autowired
	JobRepository jobRepository;

	@Resource
	private ApplicationContext context;

	@Test
	public void test() throws Exception {
		//retrieve the step from the context and see that it's allow is set
		AbstractStep abstractStep = (AbstractStep) context.getBean("simpleJob.step1");
		assertTrue(abstractStep.isAllowStartIfComplete());
	}

	@Test
	public void testRestart() throws Exception {
		JobParametersBuilder paramBuilder = new JobParametersBuilder();
		paramBuilder.addDate("value", new Date());
		JobExecution jobExecution = jobRepository.createJobExecution(job.getName(), paramBuilder.toJobParameters());

		job.execute(jobExecution);

		jobExecution = jobRepository.createJobExecution(job.getName(), paramBuilder.toJobParameters());
		job.execute(jobExecution);

		int count = jobRepository.getStepExecutionCount(jobExecution.getJobInstance(), "simpleJob.step1");
		assertEquals(2, count);
	}
}
