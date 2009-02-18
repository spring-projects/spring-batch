package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;

public class FaultTolerantChunkProcessorTests {

	private BatchRetryTemplate batchRetryTemplate = new BatchRetryTemplate();

	private List<String> list = new ArrayList<String>();

	@Test
	public void testWrite() throws Exception {
		FaultTolerantChunkProcessor<String, String> processor = new FaultTolerantChunkProcessor<String, String>(
				new PassThroughItemProcessor<String>(), new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						list.addAll(items);
					}
				}, batchRetryTemplate);
		Chunk<String> inputs = new Chunk<String>();
		inputs.add("1");
		inputs.add("2");
		processor.process(new StepExecution("foo", new JobExecution(0L)).createStepContribution(), inputs);
		assertEquals(2, list.size());
	}

	@Test
	public void testTransform() throws Exception {
		FaultTolerantChunkProcessor<String, String> processor = new FaultTolerantChunkProcessor<String, String>(
				new ItemProcessor<String, String>() {
					public String process(String item) throws Exception {
						return item.equals("1") ? null : item;
					}
				}, new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						list.addAll(items);
					}
				}, batchRetryTemplate);
		Chunk<String> inputs = new Chunk<String>();
		inputs.add("1");
		inputs.add("2");
		processor.process(new StepExecution("foo", new JobExecution(0L)).createStepContribution(), inputs);
		assertEquals(1, list.size());
	}
}
