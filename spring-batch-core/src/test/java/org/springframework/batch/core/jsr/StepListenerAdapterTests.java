package org.springframework.batch.core.jsr;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.batch.api.listener.StepListener;
import javax.batch.operations.BatchRuntimeException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;

public class StepListenerAdapterTests {

	private StepListenerAdapter adapter;
	@Mock
	private StepListener delegate;
	@Mock
	private StepExecution execution;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);

		adapter = new StepListenerAdapter(delegate);
	}

	@Test(expected=IllegalArgumentException.class)
	public void testCreateWithNull() {
		adapter = new StepListenerAdapter(null);
	}

	@Test
	public void testBeforeStep() throws Exception {
		adapter.beforeStep(null);

		verify(delegate).beforeStep();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testBeforeStepException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).beforeStep();

		adapter.beforeStep(null);
	}

	@Test
	public void testAfterStep() throws Exception {
		ExitStatus exitStatus = new ExitStatus("complete");
		when(execution.getExitStatus()).thenReturn(exitStatus);

		assertEquals(exitStatus, adapter.afterStep(execution));

		verify(delegate).afterStep();
	}

	@Test(expected=BatchRuntimeException.class)
	public void testAfterStepException() throws Exception {
		doThrow(new Exception("expected")).when(delegate).afterStep();

		adapter.afterStep(null);
	}
}
