/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.sample.quartz;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.springframework.batch.core.configuration.JobLocator;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.batch.execution.launch.JobLauncher;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * @author Dave Syer
 *
 */
public class JobLauncherDetails extends QuartzJobBean {
	
	private static Log log = LogFactory.getLog(JobLauncherDetails.class);

	private JobLocator jobLocator;
	
	private JobLauncher jobLauncher;
	
	/**
	 * Public setter for the {@link JobLocator}.
	 * @param jobLocator the {@link JobLocator} to set
	 */
	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}
	
	/**
	 * Public setter for the {@link JobLauncher}.
	 * @param jobLauncher the {@link JobLauncher} to set
	 */
	public void setJobLauncher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	protected void executeInternal(JobExecutionContext context) {
		String jobName =  (String) context.getJobDetail().getJobDataMap().get("jobName");
		JobParameters jobParameters = new JobParameters();
		try {
			jobLauncher.run(jobLocator.getJob(jobName), jobParameters);
		}
		catch (JobExecutionAlreadyRunningException e) {
			log.error("Could not execute job.", e);
		}
		catch (JobRestartException e) {
			log.error("Could not execute job.", e);
		}
		catch (NoSuchJobException e) {
			log.error("Could not execute job.", e);
		}
	}

}
