package org.springframework.batch.sample.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;

public class OutputFileListenerTests {

	private OutputFileListener listener = new OutputFileListener();
	private StepExecution stepExecution = new StepExecution("foo", new JobExecution(0L), 1L);
	
	@Test
	public void testCreateOutputNameFromInput() {
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("{outputFile=file:./target/output/foo.csv}", stepExecution.getExecutionContext().toString());
	}

	@Test
	public void testSetPath() {
		listener.setPath("spam/");
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("{outputFile=spam/foo.csv}", stepExecution.getExecutionContext().toString());
	}

	@Test
	public void testSetOutputKeyName() {
		listener.setPath("");
		listener.setOutputKeyName("spam");
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("{spam=foo.csv}", stepExecution.getExecutionContext().toString());
	}

	@Test
	public void testSetInputKeyName() {
		listener.setPath("");
		listener.setInputKeyName("spam");
		stepExecution.getExecutionContext().putString("spam", "bar");
		listener.createOutputNameFromInput(stepExecution);
		assertEquals("bar.csv", stepExecution.getExecutionContext().getString("outputFile"));
	}

}
