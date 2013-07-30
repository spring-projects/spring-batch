package org.springframework.batch.core.jsr.step.batchlet;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.batch.api.Batchlet;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.repeat.RepeatStatus;

public class BatchletAdapterTests {

	private BatchletAdapter adapter;
	@Mock
	private Batchlet delegate;
	@Mock
	private StepContribution contribution;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new BatchletAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new BatchletAdapter(null);
	}

	@Test
	public void testExecuteNoExitStatus() throws Exception {
		assertEquals(RepeatStatus.FINISHED, adapter.execute(contribution, null));

		verify(delegate).process();
	}

	@Test
	public void testExecuteWithExitStatus() throws Exception {
		when(delegate.process()).thenReturn("my exit status");

		assertEquals(RepeatStatus.FINISHED, adapter.execute(contribution, null));

		verify(delegate).process();
		verify(contribution).setExitStatus(new ExitStatus("my exit status"));
	}
}
