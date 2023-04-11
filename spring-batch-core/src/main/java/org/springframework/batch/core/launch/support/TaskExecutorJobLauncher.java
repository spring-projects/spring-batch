/*
 * Copyright 2022 the original author or authors.
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
package org.springframework.batch.core.launch.support;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Implementation of the {@link JobLauncher} interface based on a {@link TaskExecutor}.
 * This means that the type of executor set is very important. If a
 * {@link SyncTaskExecutor} is used, then the job will be processed <strong>within the
 * same thread that called the launcher.</strong> Care should be taken to ensure any users
 * of this class understand fully whether or not the implementation of TaskExecutor used
 * will start tasks synchronously or asynchronously. The default setting uses a
 * synchronous task executor.
 *
 * There is only one required dependency of this Launcher, a {@link JobRepository}. The
 * JobRepository is used to obtain a valid JobExecution. The Repository must be used
 * because the provided {@link Job} could be a restart of an existing {@link JobInstance},
 * and only the Repository can reliably recreate it.
 *
 * @author Lucas Ward
 * @author Dave Syer
 * @author Will Schipp
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 * @since 1.0
 * @see JobRepository
 * @see TaskExecutor
 */
public class TaskExecutorJobLauncher extends SimpleJobLauncher {

	@Override
	public JobExecution run(Job job, JobParameters jobParameters) throws JobExecutionAlreadyRunningException,
			JobRestartException, JobInstanceAlreadyCompleteException, JobParametersInvalidException {
		return super.run(job, jobParameters);
	}

	@Override
	public void setJobRepository(JobRepository jobRepository) {
		super.setJobRepository(jobRepository);
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		super.setTaskExecutor(taskExecutor);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();
	}

}
