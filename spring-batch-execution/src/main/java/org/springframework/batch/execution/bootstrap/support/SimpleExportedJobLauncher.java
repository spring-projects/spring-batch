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
package org.springframework.batch.execution.bootstrap.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.springframework.batch.core.domain.Job;
import org.springframework.batch.core.domain.JobExecution;
import org.springframework.batch.core.domain.JobParameters;
import org.springframework.batch.core.domain.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobLocator;
import org.springframework.batch.core.repository.NoSuchJobException;
import org.springframework.batch.core.runtime.JobParametersFactory;
import org.springframework.batch.execution.launch.JobLauncher;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * @author Dave Syer
 * 
 */
public class SimpleExportedJobLauncher implements ExportedJobLauncher, InitializingBean {

	private JobLauncher launcher;

	private JobLocator jobLocator;

	private Map registry = new HashMap();

	private JobParametersFactory jobParametersFactory = new DefaultJobParametersFactory();
	
	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(launcher, "JobLauncher must be provided.");
		Assert.notNull(jobLocator, "JobLocator must be provided.");
	}

	/**
	 * Public setter for the {@link JobLauncher}.
	 * @param launcher the launcher to set
	 */
	public void setLauncher(JobLauncher launcher) {
		this.launcher = launcher;
	}
	
	/**
	 * Public setter for the JobLocator.
	 * @param jobLocator the jobLocator to set
	 */
	public void setJobLocator(JobLocator jobLocator) {
		this.jobLocator = jobLocator;
	}
	
	/**
	 * Public setter for the JobParametersFactory.
	 * @param jobParametersFactory the jobParametersFactory to set
	 */
	public void setJobParametersFactory(JobParametersFactory jobParametersFactory) {
		this.jobParametersFactory = jobParametersFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.execution.bootstrap.support.ExportedJobLauncher#getStatistics()
	 */
	public Properties getStatistics() {
		Properties result = new Properties();
		int i = 0;
		for (Iterator iterator = registry.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			JobExecution execution = (JobExecution) registry.get(key);
			addStatistics(result, execution, "job" + i + ".");
			i++;
		}
		return result;
	}

	/**
	 * @param result
	 * @param execution
	 */
	private void addStatistics(Properties result, JobExecution execution, String prefix) {
		int i = 0;
		for (Iterator iterator = execution.getStepExecutions().iterator(); iterator.hasNext();) {
			StepExecution stepExecution = (StepExecution) iterator.next();
			Properties statistics = stepExecution.getStatistics();
			for (Iterator iter = statistics.keySet().iterator(); iter.hasNext();) {
				String key = (String) iter.next();
				result.setProperty(prefix + "step" + i + "." + key, statistics.getProperty(key));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.execution.bootstrap.support.ExportedJobLauncher#isRunning()
	 */
	public boolean isRunning() {
		for (Iterator iterator = registry.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			JobExecution execution = (JobExecution) registry.get(key);
			if (execution.isRunning()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.execution.bootstrap.support.ExportedJobLauncher#run(java.lang.String)
	 */
	public String run(String name) {
		return run(name, null);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.execution.bootstrap.support.ExportedJobLauncher#run(java.lang.String,
	 * java.lang.String)
	 */
	public String run(String name, String params) {

		Job job;
		try {
			job = jobLocator.getJob(name);
		}
		catch (NoSuchJobException e) {
			return e.getClass().getName() + ": " + e.getMessage();
		}

		JobParameters jobParameters = new JobParameters();
		if (params != null) {
			jobParameters = jobParametersFactory.getJobParameters(PropertiesConverter.stringToProperties(params));
		}

		JobExecution execution;
		try {
			execution = launcher.run(job, jobParameters);
		}
		catch (JobExecutionAlreadyRunningException e) {
			return e.getClass().getName() + ": " + e.getMessage();
		}

		registry.put(name + params, execution);

		return execution.toString();

	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.batch.execution.bootstrap.support.ExportedJobLauncher#stop()
	 */
	public void stop() {
		for (Iterator iterator = registry.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			JobExecution execution = (JobExecution) registry.get(key);
			execution.stop();
		}
		registry.clear();
	}

}
