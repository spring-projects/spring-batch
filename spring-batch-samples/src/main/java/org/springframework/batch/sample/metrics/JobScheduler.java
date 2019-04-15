package org.springframework.batch.sample.metrics;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

	private final Job job1;
	private final Job job2;
	private final JobLauncher jobLauncher;

	@Autowired
	public JobScheduler(Job job1, Job job2, JobLauncher jobLauncher) {
		this.job1 = job1;
		this.job2 = job2;
		this.jobLauncher = jobLauncher;
	}

	@Scheduled(cron="*/10 * * * * *")
	public void launchJob1() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis())
				.toJobParameters();

		jobLauncher.run(job1, jobParameters);
	}

	@Scheduled(cron="*/15 * * * * *")
	public void launchJob2() throws Exception {
		JobParameters jobParameters = new JobParametersBuilder()
				.addLong("time", System.currentTimeMillis())
				.toJobParameters();

		jobLauncher.run(job2, jobParameters);
	}

}
