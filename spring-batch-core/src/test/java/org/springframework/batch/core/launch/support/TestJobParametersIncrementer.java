package org.springframework.batch.core.launch.support;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersIncrementer;

public class TestJobParametersIncrementer implements JobParametersIncrementer {

	public JobParameters getNext(JobParameters parameters) {
		return  new JobParametersBuilder().addString("foo", "spam").toJobParameters();
	}

}
