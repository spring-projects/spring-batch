/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.batch.samples.loom;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.JvmCommandRunner;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.samples.common.DataSourceConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

/**
 * Configuration class that defines a step with a {@link SystemCommandTasklet} based on a
 * {@link VirtualThreadTaskExecutor}.
 *
 * @author Mahmoud Ben Hassine
 */
@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
@Import(DataSourceConfiguration.class)
public class JobConfigurationForRunningSystemCommandTaskletsWithVirtualThreads {

	@Bean
	public SystemCommandTasklet tasklet() {
		SystemCommandTasklet systemCommandTasklet = new SystemCommandTasklet();
		systemCommandTasklet.setCommand("java", "-version");
		systemCommandTasklet.setCommandRunner(new JvmCommandRunner() {
			@Override
			public Process exec(String[] command, String[] envp, File dir) throws IOException {
				System.out.println(Thread.currentThread() + ": running command " + Arrays.toString(command));
				return super.exec(command, envp, dir);
			}
		});
		systemCommandTasklet.setTaskExecutor(new VirtualThreadTaskExecutor("spring-batch-"));
		systemCommandTasklet.setTimeout(1000L);
		return systemCommandTasklet;
	}

	@Bean
	public Job job(JobRepository jobRepository, JdbcTransactionManager transactionManager, SystemCommandTasklet tasklet)
			throws Exception {
		Step step = new StepBuilder("step", jobRepository).tasklet(tasklet, transactionManager).build();
		return new JobBuilder("job", jobRepository).start(step).build();
	}

}