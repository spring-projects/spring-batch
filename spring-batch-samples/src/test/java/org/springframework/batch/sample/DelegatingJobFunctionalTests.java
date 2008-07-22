package org.springframework.batch.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.springframework.batch.sample.domain.PersonService;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.runner.RunWith;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration()
public class DelegatingJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	@Autowired
	private PersonService personService;
	
	protected void validatePostConditions() throws Exception {
		assertTrue(personService.getReturnedCount() > 0);
		assertEquals(personService.getReturnedCount(), personService.getReceivedCount());
		
	}
	
}
