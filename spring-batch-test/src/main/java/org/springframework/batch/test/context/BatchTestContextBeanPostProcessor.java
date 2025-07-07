/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.batch.test.context;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * {@link BeanPostProcessor} implementation that injects a job bean into
 * {@link JobLauncherTestUtils} if there is a unique job bean.
 *
 * @author Henning Pöttker
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class BatchTestContextBeanPostProcessor implements BeanPostProcessor {

	private ObjectProvider<Job> jobProvider;

	private ObjectProvider<JobRepository> jobRepositoryProvider;

	private ObjectProvider<JobLauncher> jobLauncherProvider;

	@Autowired
	public void setJobProvider(ObjectProvider<Job> jobProvider) {
		this.jobProvider = jobProvider;
	}

	@Autowired
	public void setJobRepositoryProvider(ObjectProvider<JobRepository> jobRepositoryProvider) {
		this.jobRepositoryProvider = jobRepositoryProvider;
	}

	@Autowired
	public void setJobLauncherProvider(ObjectProvider<JobLauncher> jobLauncherProvider) {
		this.jobLauncherProvider = jobLauncherProvider;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof JobLauncherTestUtils jobLauncherTestUtils) {
			this.jobProvider.ifUnique(jobLauncherTestUtils::setJob);
			this.jobRepositoryProvider.ifUnique(jobLauncherTestUtils::setJobRepository);
			this.jobLauncherProvider.ifUnique(jobLauncherTestUtils::setJobLauncher);
		}
		if (bean instanceof JobRepositoryTestUtils jobRepositoryTestUtils) {
			this.jobRepositoryProvider.ifUnique(jobRepositoryTestUtils::setJobRepository);
		}
		return bean;
	}

}
