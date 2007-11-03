package org.springframework.batch.sample;

public class NflJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	protected String[] getConfigLocations() {
		return new String[] {"jobs/nfljob.xmlXXX"};
	}
	
	protected void validatePostConditions() throws Exception {
		// TODO Auto-generated method stub

	}

}
