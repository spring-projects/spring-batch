package org.springframework.batch.sample.item.processor;

import junit.framework.TestCase;

import org.springframework.batch.io.file.FlatFileItemWriter;
import org.springframework.batch.sample.item.processor.DefaultFlatFileProcessor;

public class DefaultFlatFileProcessorTests extends TestCase {

	public void testProcess() throws Exception {
		
		final Object testLine = new Object();
		
		//create output source
		FlatFileItemWriter output = new FlatFileItemWriter() {
			public void write(Object line) {
				assertEquals(""+testLine, line);
			}
		};
		
		//create processor and set output source
		DefaultFlatFileProcessor processor = new DefaultFlatFileProcessor();
		processor.setFlatFileOutputSource(output);
		
		//call tested method - see assert in output.write() method
		processor.process(testLine);
	}
}
