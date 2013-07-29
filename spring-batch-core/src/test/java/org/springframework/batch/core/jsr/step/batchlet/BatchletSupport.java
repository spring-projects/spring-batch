package org.springframework.batch.core.jsr.step.batchlet;

import javax.batch.api.Batchlet;

public class BatchletSupport implements Batchlet {

	@Override
	public String process() throws Exception {
		return null;
	}

	@Override
	public void stop() throws Exception {
		// no-op
	}
}
