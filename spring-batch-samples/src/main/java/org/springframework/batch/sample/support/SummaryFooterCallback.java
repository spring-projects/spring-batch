package org.springframework.batch.sample.support;

import java.io.IOException;
import java.io.Writer;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.item.file.FileWriterCallback;

/**
 * Writes summary info in the footer of a file.
 */
public class SummaryFooterCallback extends StepExecutionListenerSupport implements FileWriterCallback{

	private StepExecution stepExecution;
	
	public void write(Writer writer) throws IOException {
		writer.write("footer - number of items written: " + stepExecution.getWriteCount());
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		this.stepExecution = stepExecution;
	}

	
}
