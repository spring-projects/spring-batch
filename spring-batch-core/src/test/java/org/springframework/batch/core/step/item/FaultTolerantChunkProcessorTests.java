package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.ItemListenerSupport;
import org.springframework.batch.core.step.skip.AlwaysSkipItemSkipPolicy;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.batch.retry.policy.NeverRetryPolicy;

public class FaultTolerantChunkProcessorTests {

	private BatchRetryTemplate batchRetryTemplate = new BatchRetryTemplate();

	private List<String> list = new ArrayList<String>();

	private List<String> after = new ArrayList<String>();

	private FaultTolerantChunkProcessor<String, String> processor;

	private StepContribution contribution = new StepExecution("foo", new JobExecution(0L)).createStepContribution();

	@Before
	public void setUp() {
		processor = new FaultTolerantChunkProcessor<String, String>(new PassThroughItemProcessor<String>(),
				new ItemWriter<String>() {
					public void write(List<? extends String> items) throws Exception {
						if (items.contains("fail")) {
							throw new RuntimeException("Planned failure!");
						}
						list.addAll(items);
					}
				}, batchRetryTemplate);
		batchRetryTemplate.setRetryPolicy(new NeverRetryPolicy());
	}

	@Test
	public void testWrite() throws Exception {
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("1", "2"));
		processor.process(contribution, inputs);
		assertEquals(2, list.size());
	}

	@Test
	public void testTransform() throws Exception {
		processor.setItemProcessor(new ItemProcessor<String, String>() {
			public String process(String item) throws Exception {
				return item.equals("1") ? null : item;
			}
		});
		Chunk<String> inputs = new Chunk<String>(Arrays.asList("1", "2"));
		processor.process(contribution, inputs);
		assertEquals(1, list.size());
	}

	@Test
	public void testAfterWrite() throws Exception {
		Chunk<String> chunk = new Chunk<String>(Arrays.asList("foo", "fail", "bar"));
		processor.setListeners(Arrays.asList(new ItemListenerSupport<String, String>() {
			@Override
			public void afterWrite(List<? extends String> item) {
				after.addAll(item);
			}
		}));
		processor.setWriteSkipPolicy(new AlwaysSkipItemSkipPolicy());
		try {
			processor.process(contribution, chunk);
		}
		catch (RuntimeException e) {
			assertEquals("Planned failure!", e.getMessage());
		}
		try {
			processor.process(contribution, chunk);
		}
		catch (RuntimeException e) {
			assertEquals("Planned failure!", e.getMessage());
		}
		assertEquals(2, chunk.getItems().size());
		processor.process(contribution, chunk);
		// foo is written twice because the failure is detected on the second
		// attempt when throttling
		assertEquals("[foo, foo, bar]", list.toString());
		// but the after listener is only called once, which is important
		assertEquals(2, after.size());
	}

}
