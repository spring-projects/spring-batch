/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.batch.core.jsr.launch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.batch.operations.JobExecutionAlreadyCompleteException;
import javax.batch.operations.JobExecutionIsRunningException;
import javax.batch.operations.JobExecutionNotMostRecentException;
import javax.batch.operations.JobExecutionNotRunningException;
import javax.batch.operations.JobOperator;
import javax.batch.operations.JobRestartException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobException;
import javax.batch.operations.NoSuchJobExecutionException;
import javax.batch.operations.NoSuchJobInstanceException;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;
import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;

/**
 * The entrance for executing batch jobs as defined by JSR-352.  This class provides
 * a base {@link ApplicationContext} that is the equivalent to the following:
 * 
 * <pre class="code">
 * 	&#064;Configuration
 * 	&#064;EnableBatchProcessing
 * 	public static class BaseConfiguration extends DefaultBatchConfigurer {
 * 
 * 		&#064;Bean
 * 		JobLauncher jobLauncher() { ... }
 * 
 * 		&#064;Bean
 * 		org.springframework.batch.core.launch.JobOperator batchJobOperator(JobExplorer jobExplorer,
 * 																		   JobLauncher jobLauncher,
 * 																		   JobRepository jobRepository,
 * 																		   JobRegistry jobRegistry)  { ... }
 * 
 * 		&#064;Bean
 * 		JobExplorerFactoryBean jobExplorer(final DataSource dataSource)  { ... }
 * 
 * 		&#064;Bean
 * 		DataSource dataSource()  { ... }
 * 	}
 * </pre>
 * 
 * @author Michael Minella
 * @since 3.0
 * @see EnableBatchProcessing
 */
public class JsrJobOperator implements JobOperator {

	private org.springframework.batch.core.launch.JobOperator batchJobOperator;
	private JobExplorer jobExplorer;
	private JobLauncher jobLauncher;
	private GenericApplicationContext baseContext;

	public JsrJobOperator() {
		baseContext = new AnnotationConfigApplicationContext(BaseConfiguration.class);
		jobLauncher = baseContext.getBean(JobLauncher.class);
		jobExplorer = baseContext.getBean(JobExplorer.class);
		batchJobOperator = baseContext.getBean(org.springframework.batch.core.launch.JobOperator.class);
		try {
			((SimpleJobLauncher) jobLauncher).afterPropertiesSet();
			((SimpleJobOperator) batchJobOperator).afterPropertiesSet();
		} catch (Exception e) {
		}
	}

	@Override
	public void abandon(long jobExecutionId) throws NoSuchJobExecutionException,
	JobExecutionIsRunningException, JobSecurityException {
		try {
			batchJobOperator.abandon(jobExecutionId);
		} catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobException(e);
		} catch (JobExecutionAlreadyRunningException e) {
			throw new JobExecutionIsRunningException(e);
		}
	}

	@Override
	public JobExecution getJobExecution(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution jobExecution = jobExplorer.getJobExecution(executionId);

		if(jobExecution == null) {
			throw new NoSuchJobException("No execution was found for executionId " + executionId);
		}

		return new org.springframework.batch.core.jsr.JobExecution(jobExecution);
	}

	@Override
	public List<JobExecution> getJobExecutions(JobInstance jobInstance)
			throws NoSuchJobInstanceException, JobSecurityException {
		org.springframework.batch.core.JobInstance instance = (org.springframework.batch.core.JobInstance) jobInstance;
		List<org.springframework.batch.core.JobExecution> batchExecutions = jobExplorer.getJobExecutions(instance);

		if(batchExecutions == null) {
			throw new NoSuchJobInstanceException("Unable to find JobInstance " + jobInstance.getInstanceId());
		}

		List<JobExecution> results = new ArrayList<JobExecution>(batchExecutions.size());
		for (org.springframework.batch.core.JobExecution jobExecution : batchExecutions) {
			results.add(new org.springframework.batch.core.jsr.JobExecution(jobExecution));
		}

		return results;
	}

	@Override
	public JobInstance getJobInstance(long instanceId)
			throws NoSuchJobExecutionException, JobSecurityException {
		return jobExplorer.getJobInstance(instanceId);
	}

	@Override
	public int getJobInstanceCount(String arg0) throws NoSuchJobException,
	JobSecurityException {
		return 0;
	}

	@Override
	public List<JobInstance> getJobInstances(String arg0, int arg1, int arg2)
			throws NoSuchJobException, JobSecurityException {
		return null;
	}

	@Override
	public Set<String> getJobNames() throws JobSecurityException {
		return new HashSet<String>(jobExplorer.getJobNames());
	}

	@Override
	public Properties getParameters(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution execution = jobExplorer.getJobExecution(executionId);

		if(execution == null) {
			throw new NoSuchJobExecutionException("Unable to find the JobExecution for id " + executionId);
		}

		return execution.getJobParameters().toProperties();
	}

	@Override
	public List<Long> getRunningExecutions(String name)
			throws NoSuchJobException, JobSecurityException {
		Set<org.springframework.batch.core.JobExecution> findRunningJobExecutions = jobExplorer.findRunningJobExecutions(name);
		List<Long> results = new ArrayList<Long>(findRunningJobExecutions.size());

		for (org.springframework.batch.core.JobExecution jobExecution : findRunningJobExecutions) {
			results.add(jobExecution.getId());
		}

		return results;
	}

	@Override
	public List<StepExecution> getStepExecutions(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution execution = jobExplorer.getJobExecution(executionId);

		if(execution == null) {
			throw new NoSuchJobException("JobExecution with the id " + executionId + " was not found");
		}

		return null;
		//		return execution.getStepExecutions();
	}

	@Override
	public long restart(long arg0, Properties arg1)
			throws JobExecutionAlreadyCompleteException,
			NoSuchJobExecutionException, JobExecutionNotMostRecentException,
			JobRestartException, JobSecurityException {
		return 0;
	}

	@Override
	public long start(String jobName, Properties params) throws JobStartException,
	JobSecurityException {
		GenericXmlApplicationContext batchContext = new GenericXmlApplicationContext();
		batchContext.setValidating(false);
		batchContext.load(new String[] {"/META-INF/batch.xml", "META-INF/batch-jobs/" + jobName + ".xml"});
		batchContext.setParent(baseContext);
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(AutowiredAnnotationBeanPostProcessor.class);
		batchContext.registerBeanDefinition("postProcessor", bd);
		batchContext.refresh();
		Job job = batchContext.getBean(jobName, Job.class);
		try {
			return jobLauncher.run(job, new JobParametersBuilder(params).toJobParameters()).getId();
		} catch (Exception e) {
			e.printStackTrace();
			throw new JobStartException(e);
		}
	}

	@Override
	public void stop(long executionId) throws NoSuchJobExecutionException,
	JobExecutionNotRunningException, JobSecurityException {
		try {
			batchJobOperator.stop(executionId);
		} catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobException(e);
		} catch (org.springframework.batch.core.launch.JobExecutionNotRunningException e) {
			throw new JobExecutionNotRunningException(e);
		}
	}

	@Configuration
	@EnableBatchProcessing
	public static class BaseConfiguration extends DefaultBatchConfigurer {

		@Bean
		JobLauncher jobLauncher() {
			SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
			jobLauncher.setJobRepository(super.getJobRepository());
			try {
				jobLauncher.afterPropertiesSet();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return jobLauncher;
		}

		@Bean
		org.springframework.batch.core.launch.JobOperator batchJobOperator(JobExplorer jobExplorer, JobLauncher jobLauncher, JobRepository jobRepository, JobRegistry jobRegistry) {
			SimpleJobOperator operator = new SimpleJobOperator();

			operator.setJobExplorer(jobExplorer);
			operator.setJobLauncher(jobLauncher);
			operator.setJobRepository(jobRepository);
			operator.setJobRegistry(jobRegistry);

			return operator;
		}

		@Bean
		JobExplorerFactoryBean jobExplorer(final DataSource dataSource) {
			return new JobExplorerFactoryBean() {{
				setDataSource(dataSource);
			}};
		}

		@Bean
		DataSource dataSource() {
			return new EmbeddedDatabaseBuilder().
					addScript("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql").
					addScript("classpath:org/springframework/batch/core/schema-hsqldb.sql").
					build();
		}
	}
}
