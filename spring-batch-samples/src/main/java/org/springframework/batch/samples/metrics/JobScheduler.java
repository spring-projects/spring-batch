package org.springframework.batch.samples.metrics;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

	private final Job job1;

	private final Job job2;

	private final JobOperator jobOperator;

	@Autowired
	public JobScheduler(Job job1, Job job2, JobOperator jobOperator) {
		this.job1 = job1;
		this.job2 = job2;
		this.jobOperator = jobOperator;
	}

	@Scheduled(cron = "*/10 * * * * *")
	public void launchJob1() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
			.toJobParameters();

		jobOperator.start(job1, jobParameters);
	}

	@Scheduled(cron = "*/15 * * * * *")
	public void launchJob2() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder().addLong("time", System.currentTimeMillis())
			.toJobParameters();

		jobOperator.start(job2, jobParameters);
	}

}
