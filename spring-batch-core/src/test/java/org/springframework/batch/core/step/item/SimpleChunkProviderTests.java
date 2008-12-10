package org.springframework.batch.core.step.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.support.RepeatTemplate;

public class SimpleChunkProviderTests {

	private SimpleChunkProvider<String> provider;

	private StepContribution contribution = new StepContribution(new StepExecution("foo", new JobExecution(
			new JobInstance(123L, new JobParameters(), "job"))));

	@Before
	public void setUp() {
		provider = new SimpleChunkProvider<String>(new ListItemReader<String>(Arrays.asList("foo", "bar")),
				new RepeatTemplate());
	}

	@Test
	public void testProvide() throws Exception {
		Chunk<String> chunk = provider.provide(contribution);
		assertNotNull(chunk);
		assertEquals(2, chunk.getItems().size());
	}

}
