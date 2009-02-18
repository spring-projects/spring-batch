package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;

public class SimpleChunkProcessorTests {

	private SimpleChunkProcessor<String, String> processor;

	private StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(
			new JobInstance(123L, new JobParameters(), "job"))));

	protected List<String> list = new ArrayList<String>();

	@Before
	public void setUp() {
		processor = new SimpleChunkProcessor<String,String>(new PassThroughItemProcessor<String>(), new ItemWriter<String>() {
			public void write(List<? extends String> items) throws Exception {
				list.addAll(items);
			}
		});
	}

	@Test
	public void testProcess() throws Exception {
		Chunk<String> chunk = new Chunk<String>();
		chunk.add("foo");
		chunk.add("bar");
		processor.process(contribution, chunk);
		assertEquals(2, list.size());
	}

}
