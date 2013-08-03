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
import java.util.Collection;
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
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.JobInstance;
import javax.batch.runtime.StepExecution;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.jsr.JobContext;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.util.Assert;

/**
 * The entrance for executing batch jobs as defined by JSR-352.  This class provides
 * a single base {@link ApplicationContext} that is the equivalent to the following:
 *
 * &lt;beans&gt;
 * 	&lt;batch:job-repository id="jobRepository" ... /&gt;
 *
 *  	&lt;bean id="jobLauncher" class="org.springframework.batch.core.launch.support.SimpleJobLauncher"&gt;
 *  		...
 *  	&lt;/bean&gt;
 *
 *  	&lt;bean id="batchJobOperator" class="org.springframework.batch.core.launch.support.SimpleJobOperator"&gt;
 *  		...
 *  	&lt;/bean&gt;
 *
 * 	&lt;bean id="jobExplorer" class="org.springframework.batch.core.explore.support.JobExplorerFactoryBean"&gt;
 * 		...
 * 	&lt;/bean&gt;
 *
 * 	&lt;bean id="dataSource"
 * 		class="org.apache.commons.dbcp.BasicDataSource"&gt;
 * 		...
 * 	&lt;/bean&gt;
 *
 * 	&lt;bean id="transactionManager"
 * 		class="org.springframework.jdbc.datasource.DataSourceTransactionManager"&gt;
 * 		...
 * 	&lt;/bean&gt;
 *
 * 	&lt;bean id="jobParametersConverter" class="org.springframework.batch.core.jsr.JsrJobParametersConverter"/&gt;
 *
 * 	&lt;bean id="jobRegistry" class="org.springframework.batch.core.configuration.support.MapJobRegistry"/&gt;
 *
 * 	&lt;bean id="placeholderProperties" class="org.springframework.beans.factory.config.PropertyPlaceholderConfigurer"&gt;
 * 		...
 * 	&lt;/bean&gt;
 * &lt;/beans&gt;
 *
 * Calls to {@link JobOperator#start(String, Properties)} will provide a child context to the above context
 * using the job definition and batch.xml if provided.
 *
 * By default, calls to start/restart will result in synchronous execution of the batch job (via a synchronous {@link TaskExecutor}.
 * For asynchronous behavior, a different {@link TaskExecutor} implementation is required to be provided.
 *
 * <em>Note</em>: This class is intended to only be used for JSR-352 configured jobs. Use of
 * this {@link JobOperator} to start/stop/restart Spring Batch jobs may result in unexpected behaviors due to
 * how job instances are identified differently.
 *
 * @author Michael Minella
 * @since 3.0
 */
public class JsrJobOperator implements JobOperator {

	private org.springframework.batch.core.launch.JobOperator batchJobOperator;
	private JobExplorer jobExplorer;
	private JobRepository jobRepository;
	private TaskExecutor taskExecutor;
	private JobParametersConverter jobParametersConverter;
	private static ApplicationContext baseContext;

	/**
	 * Public constructor used by {@link BatchRuntime#getJobOperator()}.  This will bootstrap a
	 * singleton ApplicationContext if one has not already been created (and will utilize the existing
	 * one if it has) to populate itself.
	 */
	public JsrJobOperator() {
		BeanFactoryLocator beanFactoryLocactor = ContextSingletonBeanFactoryLocator.getInstance();
		BeanFactoryReference ref = beanFactoryLocactor.useBeanFactory("baseContext");
		baseContext = (ApplicationContext) ref.getFactory();

		baseContext.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

		if(taskExecutor == null) {
			taskExecutor = new SyncTaskExecutor();
		}
	}

	/**
	 * The no-arg constructor is used by the {@link BatchRuntime#getJobOperator()} and so bootstraps
	 * an {@link ApplicationContext}.  This constructor does not and is therefore dependency injection
	 * friendly.  Also useful for unit testing.
	 *
	 * @param jobExplorer an instance of Spring Batch's {@link JobExplorer}
	 * @param jobRepository an instance of Spring Batch's {@link JobOperator}
	 * @param jobOperator an instance of Spring Batch's {@link org.springframework.batch.core.launch.JobOperator}
	 */
	public JsrJobOperator(JobExplorer jobExplorer, JobRepository jobRepository, org.springframework.batch.core.launch.JobOperator jobOperator, JobParametersConverter jobParametersConverter) {
		Assert.notNull(jobExplorer, "A JobExplorer is required");
		Assert.notNull(jobRepository, "A JobRepository is required");
		Assert.notNull(jobOperator, "A JobOperator is required");
		Assert.notNull(jobParametersConverter, "A ParametersConverter is required");

		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
		this.batchJobOperator = jobOperator;
		this.jobParametersConverter = jobParametersConverter;
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		Assert.notNull(jobExplorer, "A JobExplorer is required");

		this.jobExplorer = jobExplorer;
	}

	public void setJobRepository(JobRepository jobRepository) {
		Assert.notNull(jobRepository, "A JobRepository is required");

		this.jobRepository = jobRepository;
	}

	public void setJobOperator(org.springframework.batch.core.launch.JobOperator jobOperator) {
		Assert.notNull(jobOperator, "A JobOperator is required");

		this.batchJobOperator = jobOperator;
	}

	/**
	 * Used to convert the {@link Properties} objects used by JSR-352 to the {@link JobParameters}
	 * objects used in Spring Batch.  The default implementation used will configure all parameters
	 * to be non-identifying (per the JSR).
	 *
	 * @param converter A {@link Converter} implementation used to convert {@link Properties} to
	 * {@link JobParameters}
	 */
	public void setJobParametersConverter(JobParametersConverter converter) {
		Assert.notNull(converter, "A Converter is required");

		this.jobParametersConverter = converter;
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#abandon(long)
	 */
	@Override
	public void abandon(long jobExecutionId) throws NoSuchJobExecutionException,
	JobExecutionIsRunningException, JobSecurityException {
		try {
			batchJobOperator.abandon(jobExecutionId);
		} catch (org.springframework.batch.core.launch.NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(e);
		} catch (JobExecutionAlreadyRunningException e) {
			throw new JobExecutionIsRunningException(e);
		}
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobExecution(long)
	 */
	@Override
	public JobExecution getJobExecution(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution jobExecution = jobExplorer.getJobExecution(executionId);

		if(jobExecution == null) {
			throw new NoSuchJobExecutionException("No execution was found for executionId " + executionId);
		}

		return new org.springframework.batch.core.jsr.JobExecution(jobExecution, jobParametersConverter);
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobExecutions(javax.batch.runtime.JobInstance)
	 */
	@Override
	public List<JobExecution> getJobExecutions(JobInstance jobInstance)
			throws NoSuchJobInstanceException, JobSecurityException {
		if(jobInstance == null) {
			throw new NoSuchJobInstanceException("A null JobInstance was provided");
		}

		org.springframework.batch.core.JobInstance instance = (org.springframework.batch.core.JobInstance) jobInstance;
		List<org.springframework.batch.core.JobExecution> batchExecutions = jobExplorer.getJobExecutions(instance);

		if(batchExecutions == null || batchExecutions.size() == 0) {
			throw new NoSuchJobInstanceException("Unable to find JobInstance " + jobInstance.getInstanceId());
		}

		List<JobExecution> results = new ArrayList<JobExecution>(batchExecutions.size());
		for (org.springframework.batch.core.JobExecution jobExecution : batchExecutions) {
			results.add(new org.springframework.batch.core.jsr.JobExecution(jobExecution, jobParametersConverter));
		}

		return results;
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobInstance(long)
	 */
	@Override
	public JobInstance getJobInstance(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution execution = jobExplorer.getJobExecution(executionId);

		if(execution == null) {
			throw new NoSuchJobExecutionException("The JobExecution was not found");
		}

		return jobExplorer.getJobInstance(execution.getJobInstance().getId());
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobInstanceCount(java.lang.String)
	 */
	@Override
	public int getJobInstanceCount(String jobName) throws NoSuchJobException,
	JobSecurityException {
		try {
			return jobExplorer.getJobInstanceCount(jobName);
		} catch (org.springframework.batch.core.launch.NoSuchJobException e) {
			throw new NoSuchJobException("No job instances were found for job name " + jobName);
		}
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobInstances(java.lang.String, int, int)
	 */
	@Override
	public List<JobInstance> getJobInstances(String jobName, int start, int count)
			throws NoSuchJobException, JobSecurityException {
		List<org.springframework.batch.core.JobInstance> jobInstances = jobExplorer.getJobInstances(jobName, start, count);

		if(jobInstances == null || jobInstances.size() == 0) {
			throw new NoSuchJobException("The job was not found");
		}

		return new ArrayList<JobInstance>(jobInstances);
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobNames()
	 */
	@Override
	public Set<String> getJobNames() throws JobSecurityException {
		return new HashSet<String>(jobExplorer.getJobNames());
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getParameters(long)
	 */
	@Override
	public Properties getParameters(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution execution = jobExplorer.getJobExecution(executionId);

		if(execution == null) {
			throw new NoSuchJobExecutionException("Unable to find the JobExecution for id " + executionId);
		}

		return jobParametersConverter.getProperties(execution.getJobParameters());
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getRunningExecutions(java.lang.String)
	 */
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

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getStepExecutions(long)
	 */
	@Override
	public List<StepExecution> getStepExecutions(long executionId)
			throws NoSuchJobExecutionException, JobSecurityException {
		org.springframework.batch.core.JobExecution execution = jobExplorer.getJobExecution(executionId);

		if(execution == null) {
			throw new NoSuchJobException("JobExecution with the id " + executionId + " was not found");
		}

		Collection<org.springframework.batch.core.StepExecution> executions = execution.getStepExecutions();

		List<StepExecution> batchExecutions = new ArrayList<StepExecution>();

		if(executions != null) {
			for (org.springframework.batch.core.StepExecution stepExecution : executions) {
				batchExecutions.add(new org.springframework.batch.core.jsr.StepExecution(jobExplorer.getStepExecution(executionId, stepExecution.getId())));
			}
		}

		return batchExecutions;
	}

	/**
	 * Creates a child {@link ApplicationContext} for the job being requested based upon
	 * the /META-INF/batch.xml (if exists) and the /META-INF/batch-jobs/&lt;jobName&gt;.xml
	 * configuration and restart the job.
	 *
	 * @param executionId the database id of the job execution to be restarted.
	 * @param params any job parameters to be used during the execution of this job.
	 * @throws JobExecutionAlreadyCompleteException thrown if the requested job execution has
	 * a status of COMPLETE
	 * @throws NoSuchJobExecutionException throw if the requested job execution does not exist
	 * in the repository
	 * @throws JobExecutionNotMostRecentException thrown if the requested job execution is not
	 * the most recent attempt for the job instance it's related to.
	 * @throws JobRestartException thrown for any general errors during the job restart process
	 */
	@Override
	@SuppressWarnings("resource")
	public long restart(long executionId, Properties params)
			throws JobExecutionAlreadyCompleteException,
			NoSuchJobExecutionException, JobExecutionNotMostRecentException,
			JobRestartException, JobSecurityException {

		org.springframework.batch.core.JobExecution previousJobExecution = jobExplorer.getJobExecution(executionId);

		if (previousJobExecution == null) {
			throw new NoSuchJobExecutionException("No JobExecution found for id: [" + executionId + "]");
		} else if(previousJobExecution.getStatus().equals(BatchStatus.COMPLETED)) {
			throw new JobExecutionAlreadyCompleteException("The requested job has already completed");
		}

		String jobName = previousJobExecution.getJobInstance().getJobName();

		GenericXmlApplicationContext batchContext = new GenericXmlApplicationContext();
		batchContext.setValidating(false);

		Resource batchXml = new ClassPathResource("/META-INF/batch.xml");
		Resource jobXml = new ClassPathResource(previousJobExecution.getJobConfigurationName());

		if(batchXml.exists()) {
			batchContext.load(batchXml);
		}

		if(jobXml.exists()) {
			batchContext.load(jobXml);
		}

		batchContext.setParent(baseContext);
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(AutowiredAnnotationBeanPostProcessor.class);
		batchContext.registerBeanDefinition("postProcessor", bd);
		batchContext.refresh();
		final Job job = batchContext.getBean(Job.class);

		if(!job.isRestartable()) {
			throw new JobRestartException("Job " + jobName + " is not restartable");
		}

		final org.springframework.batch.core.JobExecution jobExecution;

		try {
			JobParameters jobParameters = jobParametersConverter.getJobParameters(params);
			jobExecution = jobRepository.createJobExecution(previousJobExecution.getJobInstance(), jobParameters, previousJobExecution.getJobConfigurationName());
		} catch (Exception e) {
			throw new JobRestartException(e);
		}

		try {
			ConfigurableListableBeanFactory factory = ((ConfigurableApplicationContext)batchContext).getBeanFactory();
			factory.registerSingleton(job.getName() + "_" + jobExecution.getId() + "_jobContext", new JobContext(jobExecution, jobParametersConverter));

			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						job.execute(jobExecution);
					}
					catch (Throwable t) {
						throw new JobRestartException(t);
					}
				}
			});
		}
		catch (TaskRejectedException e) {
			jobExecution.upgradeStatus(BatchStatus.FAILED);
			if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
				jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}
			jobRepository.update(jobExecution);
		}

		batchContext.close();

		return jobExecution.getId();
	}

	/**
	 * Creates a child {@link ApplicationContext} for the job being requested based upon
	 * the /META-INF/batch.xml (if exists) and the /META-INF/batch-jobs/&lt;jobName&gt;.xml
	 * configuration and launches the job.  Per JSR-352, calls to this method will always
	 * create a new {@link JobInstance} (and related {@link JobExecution}).
	 *
	 * @param jobName the name of the job XML file without the .xml that is located within the
	 * /META-INF/batch-jobs directory.
	 * @param params any job parameters to be used during the execution of this job.
	 */
	@Override
	@SuppressWarnings("resource")
	public long start(String jobName, Properties params) throws JobStartException,
	JobSecurityException {
		GenericXmlApplicationContext batchContext = new GenericXmlApplicationContext();
		batchContext.setValidating(false);

		Resource batchXml = new ClassPathResource("/META-INF/batch.xml");
		String jobConfigurationLocation = "/META-INF/batch-jobs/" + jobName + ".xml";
		Resource jobXml = new ClassPathResource(jobConfigurationLocation);

		if(batchXml.exists()) {
			batchContext.load(batchXml);
		}

		if(jobXml.exists()) {
			batchContext.load(jobXml);
		}

		batchContext.setParent(baseContext);
		GenericBeanDefinition bd = new GenericBeanDefinition();
		bd.setBeanClass(AutowiredAnnotationBeanPostProcessor.class);
		batchContext.registerBeanDefinition("postProcessor", bd);
		batchContext.refresh();
		final Job job = batchContext.getBean(Job.class);

		Assert.notNull(jobName, "The job name must not be null.");

		final org.springframework.batch.core.JobExecution jobExecution;

		try {
			JobParameters jobParameters = jobParametersConverter.getJobParameters(params);
			org.springframework.batch.core.JobInstance jobInstance = jobRepository.createJobInstance(job.getName(), jobParameters);
			jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, jobConfigurationLocation);
		} catch (Exception e) {
			throw new JobStartException(e);
		}

		try {
			ConfigurableListableBeanFactory factory = ((ConfigurableApplicationContext)batchContext).getBeanFactory();
			factory.registerSingleton(job.getName() + "_" + jobExecution.getId() + "_jobContext", new JobContext(jobExecution, jobParametersConverter));

			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					try {
						job.execute(jobExecution);
					}
					catch (Throwable t) {
						throw new JobStartException(t);
					}
				}
			});
		}
		catch (TaskRejectedException e) {
			jobExecution.upgradeStatus(BatchStatus.FAILED);
			if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
				jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}
			jobRepository.update(jobExecution);
		}

		batchContext.close();

		return jobExecution.getId();
	}

	/**
	 * Delegates to {@link org.springframework.batch.core.launch.JobOperator#stop(long)}
	 *
	 * @param executionId the database id for the {@link JobExecution} to be stopped.
	 * @throws NoSuchJobExecutionException
	 * @throws JobExecutionNotRunningException
	 */
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
}
