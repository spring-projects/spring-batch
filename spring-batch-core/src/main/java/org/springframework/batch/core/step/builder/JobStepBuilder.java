/*
 * Copyright 2006-2011 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.step.job.JobParametersExtractor;
import org.springframework.batch.core.step.job.JobStep;

/**
 * A step builder for {@link JobStep} instances. A job step executes a nested {@link Job} with parameters taken from the
 * parent job or from the step execution.
 * 
 * @author Dave Syer
 * 
 * @since 2.2
 */
public class JobStepBuilder extends StepBuilderHelper<JobStepBuilder> {

	private Job job;

	private JobLauncher jobLauncher;

	private JobParametersExtractor jobParametersExtractor;

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	public JobStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * Provide a job to execute during the step.
	 * 
	 * @param job the job to execute
	 * @return this for fluent chaining
	 */
	public JobStepBuilder job(Job job) {
		this.job = job;
		return this;
	}

	/**
	 * Add a job launcher. Defaults to a simple job launcher.
	 * 
	 * @param jobLauncher the job launcher to use
	 * @return this for fluent chaining
	 */
	public JobStepBuilder launcher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
		return this;
	}

	/**
	 * Provide a job parameters extractor. Useful for extracting job parameters from the parent step execution context
	 * or job parameters.
	 * 
	 * @param jobParametersExtractor the job parameters extractor to use
	 * @return this for fluent chaining
	 */
	public JobStepBuilder parametersExtractor(JobParametersExtractor jobParametersExtractor) {
		this.jobParametersExtractor = jobParametersExtractor;
		return this;
	}

	/**
	 * Build a step from the job provided.
	 * 
	 * @return a new job step
	 */
	public Step build() {

		JobStep step = new JobStep();
		step.setName(getName());
		super.enhance(step);
		if (job != null) {
			step.setJob(job);
		}
		if (jobParametersExtractor != null) {
			step.setJobParametersExtractor(jobParametersExtractor);
		}
		if (jobLauncher == null) {
			SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
			jobLauncher.setJobRepository(getJobRepository());
			try {
				jobLauncher.afterPropertiesSet();
			}
			catch (Exception e) {
				throw new StepBuilderException(e);
			}
			this.jobLauncher = jobLauncher;
		}
		step.setJobLauncher(jobLauncher);
		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}
		return step;

	}

}
