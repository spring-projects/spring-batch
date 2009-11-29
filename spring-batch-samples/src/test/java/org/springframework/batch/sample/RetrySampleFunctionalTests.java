package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.trade.internal.GeneratingTradeItemReader;
import org.springframework.batch.sample.support.RetrySampleItemWriter;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Checks that expected number of items have been processed.
 * 
 * @author Robert Kasanicky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/retrySample.xml",
		"/job-runner-context.xml" })
public class RetrySampleFunctionalTests {

	@Autowired
	private GeneratingTradeItemReader itemGenerator;
	
	@Autowired
	private RetrySampleItemWriter<?> itemProcessor;
	
	@Autowired
	private JobLauncherTestUtils jobLauncherTestUtils;

	@Test
	public void testLaunchJob() throws Exception {
		jobLauncherTestUtils.launchJob();
		//items processed = items read + 2 exceptions
		assertEquals(itemGenerator.getLimit()+2, itemProcessor.getCounter());
	}
	
}
