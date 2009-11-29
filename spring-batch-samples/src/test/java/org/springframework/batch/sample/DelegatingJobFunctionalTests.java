package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.sample.domain.person.PersonService;
import org.springframework.batch.test.JobRunnerTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/simple-job-launcher-context.xml", "/jobs/delegatingJob.xml", "/job-runner-context.xml" })
public class DelegatingJobFunctionalTests {

	@Autowired
	private JobRunnerTestUtils jobRunnerUtils;

	@Autowired
	private PersonService personService;
	
	@Test
	public void testLaunchJob() throws Exception {
		
		jobRunnerUtils.launchJob();
		
		assertTrue(personService.getReturnedCount() > 0);
		assertEquals(personService.getReturnedCount(), personService.getReceivedCount());
		
	}
	
}
