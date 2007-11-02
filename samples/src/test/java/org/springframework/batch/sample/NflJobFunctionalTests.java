package org.springframework.batch.sample;

public class NflJobFunctionalTests extends AbstractLifecycleSpringContextTests {

	protected String[] getConfigLocations() {
		return new String[] {"jobs/nfljob.xml"};
	}
	
	protected void validatePostConditions() throws Exception {
		// TODO Auto-generated method stub

	}

}
