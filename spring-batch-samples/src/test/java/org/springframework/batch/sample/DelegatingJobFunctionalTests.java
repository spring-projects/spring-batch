package org.springframework.batch.sample;

import org.springframework.batch.sample.domain.PersonService;

public class DelegatingJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	private PersonService personService;
	
	protected void validatePostConditions() throws Exception {
		assertTrue(personService.getReturnedCount() > 0);
		assertEquals(personService.getReturnedCount(), personService.getReceivedCount());
		
	}

	// setter for auto-injection
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	
}
