package org.springframework.batch.sample;

public class FootballJobFunctionalTests extends AbstractValidatingBatchLauncherTests {

	protected String[] getConfigLocations() {
		return new String[] {"jobs/footballJob.xml"};
	}
	
	protected void validatePostConditions() throws Exception {
		// TODO Auto-generated method stub

	}

}
