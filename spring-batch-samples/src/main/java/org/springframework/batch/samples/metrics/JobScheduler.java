/*
 * Copyright 2022-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.samples.metrics;

import io.prometheus.metrics.exporter.pushgateway.PushGateway;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobScheduler {

	private static final Log LOGGER = LogFactory.getLog(JobScheduler.class);

	private final Job job1;

	private final Job job2;

	private final JobOperator jobOperator;

	private final PushGateway pushGateway;

	@Autowired
	public JobScheduler(Job job1, Job job2, JobOperator jobOperator, PushGateway pushGateway) {
		this.job1 = job1;
		this.job2 = job2;
		this.jobOperator = jobOperator;
		this.pushGateway = pushGateway;
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

	@Scheduled(fixedRateString = "${prometheus.push.rate}")
	public void pushMetrics() {
		try {
			pushGateway.pushAdd();
		}
		catch (Throwable ex) {
			LOGGER.error("Unable to push metrics to Prometheus Push Gateway", ex);
		}
	}

}
