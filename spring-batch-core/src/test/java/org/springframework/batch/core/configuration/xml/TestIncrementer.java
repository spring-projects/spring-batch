package org.springframework.batch.core.configuration.xml;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersIncrementer;

public class TestIncrementer implements JobParametersIncrementer{

	public JobParameters getNext(JobParameters parameters) {
		return null;
	}

}
