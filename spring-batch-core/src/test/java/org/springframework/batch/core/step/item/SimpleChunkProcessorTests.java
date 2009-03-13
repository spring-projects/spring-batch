package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;

public class SimpleChunkProcessorTests {

	private SimpleChunkProcessor<String, String> processor = new SimpleChunkProcessor<String, String>(
			new ItemProcessor<String, String>() {
				public String process(String item) throws Exception {
					if (item.equals("err")) {
						return null;
					}
					return item;
				}
			}, new ItemWriter<String>() {
				public void write(List<? extends String> items) throws Exception {
					if (items.contains("fail")) {
						throw new RuntimeException("Planned failure!");
					}
					list.addAll(items);
				}
			});

	private StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(
			new JobInstance(123L, new JobParameters(), "job"))));

	private List<String> list = new ArrayList<String>();

	@Before
	public void setUp() {
		list.clear();
	}

	@Test
	public void testProcess() throws Exception {
		Chunk<String> chunk = new Chunk<String>();
		chunk.add("foo");
		chunk.add("err");
		chunk.add("bar");
		processor.process(contribution, chunk);
		assertEquals(Arrays.asList("foo", "bar"), list);
		assertEquals(1, contribution.getFilterCount());
		assertEquals(2, contribution.getWriteCount());
	}

}
