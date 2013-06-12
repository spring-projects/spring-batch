package org.springframework.batch.core.jsr.configuration.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration({"SimpleItemBasedJobParsingTests-context.xml", "jsr-base-context.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class SimpleItemBasedJobParsingTests {

	@Autowired
	public Job job;

	@Autowired
	public Step step1;

	@Autowired
	public CountingItemProcessor processor;

	@Autowired
	public CountingCompletionPolicy policy;

	@Autowired
	public JobLauncher jobLauncher;

	@Test
	public void test() throws Exception {
		assertNotNull(job);
		assertEquals("job1", job.getName());
		assertNotNull(step1);
		assertEquals("step1", step1.getName());

		JobExecution execution = jobLauncher.run(job, new JobParameters());
		assertEquals(BatchStatus.COMPLETED, execution.getStatus());
		assertEquals(3, execution.getStepExecutions().size());
		assertEquals(2, processor.count);
		assertEquals(3, policy.counter);
	}

	public static class CountingItemProcessor implements ItemProcessor<String, String>{
		protected int count = 0;

		@Override
		public String process(String item) throws Exception {
			count++;
			return item;
		}
	}

	public static class CountingCompletionPolicy implements CompletionPolicy {

		protected int counter;

		@Override
		public boolean isComplete(RepeatContext context, RepeatStatus result) {
			return counter == 3;
		}

		@Override
		public boolean isComplete(RepeatContext context) {
			return counter == 3;
		}

		@Override
		public RepeatContext start(RepeatContext parent) {
			counter = 0;
			return parent;
		}

		@Override
		public void update(RepeatContext context) {
			counter++;
		}
	}
}
