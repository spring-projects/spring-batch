package org.springframework.batch.core.jsr.step.batchlet;

import javax.batch.api.Batchlet;

public class RestartBatchlet implements Batchlet {

	private static int runCount = 0;

	@Override
	public String process() throws Exception {
		runCount++;

		if(runCount == 1) {
			throw new RuntimeException("This is expected");
		}

		return null;
	}

	@Override
	public void stop() throws Exception {
	}
}
