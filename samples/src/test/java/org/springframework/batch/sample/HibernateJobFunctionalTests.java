package org.springframework.batch.sample;

/**
 * Test for HibernateJob - checks that customer credit has been updated to expected value.
 * 
 * @author Robert Kasanicky
 */
public class HibernateJobFunctionalTests extends AbstractCustomerCreditIncreaseTests {

	protected String[] getConfigLocations() {
		return new String[] {"jobs/hibernateJob.xml"};
	}
}
