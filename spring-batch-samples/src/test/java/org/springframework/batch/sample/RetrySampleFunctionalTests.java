package org.springframework.batch.sample;

import org.springframework.batch.sample.item.reader.GeneratingItemReader;
import org.springframework.batch.sample.item.writer.RetrySampleItemWriter;

/**
 * Checks that expected number of items have been processed.
 * 
 * @author Robert Kasanicky
 */
public class RetrySampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private GeneratingItemReader itemGenerator;
	
	private RetrySampleItemWriter itemProcessor;
	
	protected void validatePostConditions() throws Exception {
		//items processed = items read + 2 exceptions
		assertEquals(itemGenerator.getLimit()+2, itemProcessor.getCounter());
	}
	
	public void setItemGenerator(GeneratingItemReader itemGenerator) {
		this.itemGenerator = itemGenerator;
	}

	public void setItemProcessor(RetrySampleItemWriter itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

}
