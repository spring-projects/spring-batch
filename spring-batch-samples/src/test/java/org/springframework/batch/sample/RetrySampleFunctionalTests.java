package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.internal.GeneratingTradeItemReader;
import org.springframework.batch.sample.support.RetrySampleItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Checks that expected number of items have been processed.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class RetrySampleFunctionalTests extends AbstractValidatingBatchLauncherTests {

	@Autowired
	private GeneratingTradeItemReader itemGenerator;
	
	@Autowired
	private RetrySampleItemWriter<?> itemProcessor;
	
	protected void validatePostConditions() throws Exception {
		//items processed = items read + 2 exceptions
		assertEquals(itemGenerator.getLimit()+2, itemProcessor.getCounter());
	}
	
}
