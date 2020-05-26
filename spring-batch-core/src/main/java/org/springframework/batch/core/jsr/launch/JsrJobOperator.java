/*
 * Copyright 2013-2019 the original author or authors.
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
package org.springframework.batch.core.jsr.launch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import javax.batch.operations.BatchRuntimeException;
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.jsr.JsrJobContextFactoryBean;
import org.springframework.batch.core.jsr.JsrJobExecution;
import org.springframework.batch.core.jsr.JsrJobParametersConverter;
import org.springframework.batch.core.jsr.JsrStepExecution;
import org.springframework.batch.core.jsr.configuration.xml.JsrXmlApplicationContext;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.NoSuchStepException;
import org.springframework.batch.core.step.StepLocator;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericXmlApplicationContext;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
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
 * 		class="org.apache.commons.dbcp2.BasicDataSource"&gt;
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
 * A custom configuration of the above components can be specified by providing a system property JSR-352-BASE-CONTEXT.
 * The location that is provided by this system property will override any beans as defined in baseContext.xml.
 *
 * Calls to {@link JobOperator#start(String, Properties)} will provide a child context to the above context
 * using the job definition and batch.xml if provided.
 *
 * By default, calls to start/restart will result in asynchronous execution of the batch job (via an asynchronous {@link TaskExecutor}.
 * For synchronous behavior or customization of thread behavior, a different {@link TaskExecutor} implementation is required to
 * be provided.
 *
 * <em>Note</em>: This class is intended to only be used for JSR-352 configured jobs. Use of
 * this {@link JobOperator} to start/stop/restart Spring Batch jobs may result in unexpected behaviors due to
 * how job instances are identified differently.
 *
 * @author Michael Minella
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
 * @since 3.0
 */
public class JsrJobOperator implements JobOperator, ApplicationContextAware, InitializingBean {
	private static final String JSR_JOB_CONTEXT_BEAN_NAME = "jsr_jobContext";
	private final Log logger = LogFactory.getLog(getClass());

	private JobExplorer jobExplorer;
	private JobRepository jobRepository;
	private TaskExecutor taskExecutor;
	private JobParametersConverter jobParametersConverter;
	private ApplicationContext baseContext;
	private PlatformTransactionManager transactionManager;
	private static ExecutingJobRegistry jobRegistry = new ExecutingJobRegistry();

	/**
	 * Public constructor used by {@link BatchRuntime#getJobOperator()}.  This will bootstrap a
	 * singleton ApplicationContext if one has not already been created (and will utilize the existing
	 * one if it has) to populate itself.
	 */
	public JsrJobOperator() {

		this.baseContext = BaseContextHolder.getInstance().getContext();

		baseContext.getAutowireCapableBeanFactory().autowireBeanProperties(this,
				AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, false);

		if(taskExecutor == null) {
			taskExecutor = new SimpleAsyncTaskExecutor();
		}
	}

	/**
	 * The no-arg constructor is used by the {@link BatchRuntime#getJobOperator()} and so bootstraps
	 * an {@link ApplicationContext}.  This constructor does not and is therefore dependency injection
	 * friendly.  Also useful for unit testing.
	 *
	 * @param jobExplorer an instance of Spring Batch's {@link JobExplorer}.
	 * @param jobRepository an instance of Spring Batch's {@link JobOperator}.
	 * @param jobParametersConverter an instance of Spring Batch's {@link JobParametersConverter}.
	 * @param transactionManager a {@link PlatformTransactionManager}.
	 */
	public JsrJobOperator(JobExplorer jobExplorer, JobRepository jobRepository, JobParametersConverter jobParametersConverter, PlatformTransactionManager transactionManager) {
		Assert.notNull(jobExplorer, "A JobExplorer is required");
		Assert.notNull(jobRepository, "A JobRepository is required");
		Assert.notNull(jobParametersConverter, "A ParametersConverter is required");
		Assert.notNull(transactionManager, "A PlatformTransactionManager is required");

		this.jobExplorer = jobExplorer;
		this.jobRepository = jobRepository;
		this.jobParametersConverter = jobParametersConverter;
		this.transactionManager = transactionManager;
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		Assert.notNull(jobExplorer, "A JobExplorer is required");

		this.jobExplorer = jobExplorer;
	}

	public void setJobRepository(JobRepository jobRepository) {
		Assert.notNull(jobRepository, "A JobRepository is required");

		this.jobRepository = jobRepository;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		Assert.notNull(transactionManager, "A PlatformTransactionManager is required");

		this.transactionManager = transactionManager;
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.taskExecutor == null) {
			this.taskExecutor = new SimpleAsyncTaskExecutor();
		}
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
		org.springframework.batch.core.JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

		if(jobExecution == null) {
			throw new NoSuchJobExecutionException("Unable to retrieve JobExecution for id " + jobExecutionId);
		}

		if(jobExecution.isRunning()) {
			throw new JobExecutionIsRunningException("Unable to abandon a job that is currently running");
		}

		jobExecution.upgradeStatus(BatchStatus.ABANDONED);
		jobRepository.update(jobExecution);
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

		return new JsrJobExecution(jobExecution, jobParametersConverter);
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

		List<JobExecution> results = new ArrayList<>(batchExecutions.size());
		for (org.springframework.batch.core.JobExecution jobExecution : batchExecutions) {
			results.add(new JsrJobExecution(jobExecution, jobParametersConverter));
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
			int count = jobExplorer.getJobInstanceCount(jobName);

			if(count <= 0) {
				throw new NoSuchJobException("No job instances were found for job name " + jobName);
			} else {
				return count;
			}
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

		return new ArrayList<>(jobInstances);
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getJobNames()
	 */
	@Override
	public Set<String> getJobNames() throws JobSecurityException {
		return new HashSet<>(jobExplorer.getJobNames());
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

		Properties properties = jobParametersConverter.getProperties(execution.getJobParameters());
		properties.remove(JsrJobParametersConverter.JOB_RUN_ID);

		return properties;
	}

	/* (non-Javadoc)
	 * @see javax.batch.operations.JobOperator#getRunningExecutions(java.lang.String)
	 */
	@Override
	public List<Long> getRunningExecutions(String name)
			throws NoSuchJobException, JobSecurityException {
		Set<org.springframework.batch.core.JobExecution> findRunningJobExecutions = jobExplorer.findRunningJobExecutions(name);

		if(findRunningJobExecutions.isEmpty()) {
			throw new NoSuchJobException("Job name: " + name + " not found.");
		}

		List<Long> results = new ArrayList<>(findRunningJobExecutions.size());

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

		List<StepExecution> batchExecutions = new ArrayList<>();

		if(executions != null) {
			for (org.springframework.batch.core.StepExecution stepExecution : executions) {
				if(!stepExecution.getStepName().contains(":partition")) {
					batchExecutions.add(new JsrStepExecution(jobExplorer.getStepExecution(executionId, stepExecution.getId())));
				}
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

		List<org.springframework.batch.core.JobExecution> previousExecutions = jobExplorer.getJobExecutions(previousJobExecution.getJobInstance());

		for (org.springframework.batch.core.JobExecution jobExecution : previousExecutions) {
			if(jobExecution.getCreateTime().compareTo(previousJobExecution.getCreateTime()) > 0) {
				throw new JobExecutionNotMostRecentException("The requested JobExecution to restart was not the most recently run");
			}

			if(jobExecution.getStatus().equals(BatchStatus.ABANDONED)) {
				throw new JobRestartException("JobExecution ID: " + jobExecution.getId() + " is abandoned and attempted to be restarted.");
			}
		}

		final String jobName = previousJobExecution.getJobInstance().getJobName();

		Properties jobRestartProperties = getJobRestartProperties(params, previousJobExecution);

		final JsrXmlApplicationContext batchContext = new JsrXmlApplicationContext(jobRestartProperties);
		batchContext.setValidating(false);

		Resource batchXml = new ClassPathResource("/META-INF/batch.xml");
		Resource jobXml = new ClassPathResource(previousJobExecution.getJobConfigurationName());

		if(batchXml.exists()) {
			batchContext.load(batchXml);
		}

		if(jobXml.exists()) {
			batchContext.load(jobXml);
		}

		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.jsr.JsrJobContextFactoryBean").getBeanDefinition();
		beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
		batchContext.registerBeanDefinition(JSR_JOB_CONTEXT_BEAN_NAME, beanDefinition);

		batchContext.setParent(baseContext);

		try {
			batchContext.refresh();
		} catch (BeanCreationException e) {
			throw new JobRestartException(e);
		}

		final org.springframework.batch.core.JobExecution jobExecution;

		try {
			JobParameters jobParameters = jobParametersConverter.getJobParameters(jobRestartProperties);
			jobExecution = jobRepository.createJobExecution(previousJobExecution.getJobInstance(), jobParameters, previousJobExecution.getJobConfigurationName());
		} catch (Exception e) {
			throw new JobRestartException(e);
		}

		try {
			final Semaphore semaphore = new Semaphore(1);
			final List<Exception> exceptionHolder = Collections.synchronizedList(new ArrayList<>());
			semaphore.acquire();

			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					JsrJobContextFactoryBean factoryBean = null;
					try {
						factoryBean = (JsrJobContextFactoryBean) batchContext.getBean("&" + JSR_JOB_CONTEXT_BEAN_NAME);
						factoryBean.setJobExecution(jobExecution);
						final Job job = batchContext.getBean(Job.class);

						if(!job.isRestartable()) {
							throw new JobRestartException("Job " + jobName + " is not restartable");
						}

						semaphore.release();
						// Initialization of the JobExecution for job level dependencies
						jobRegistry.register(job, jobExecution);
						job.execute(jobExecution);
						jobRegistry.remove(jobExecution);
					}
					catch (Exception e) {
						exceptionHolder.add(e);
					} finally {
						if(factoryBean != null) {
							factoryBean.close();
						}

						batchContext.close();

						if(semaphore.availablePermits() == 0) {
							semaphore.release();
						}
					}
				}
			});

			semaphore.acquire();
			if(exceptionHolder.size() > 0) {
				semaphore.release();
				throw new JobRestartException(exceptionHolder.get(0));
			}
		}
		catch (Exception e) {
			jobExecution.upgradeStatus(BatchStatus.FAILED);
			if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
				jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}

			jobRepository.update(jobExecution);

			if(batchContext.isActive()) {
				batchContext.close();
			}

			throw new JobRestartException(e);
		}

		return jobExecution.getId();
	}

	protected Properties getJobRestartProperties(Properties params, org.springframework.batch.core.JobExecution previousJobExecution) {
		Properties jobRestartProperties = new Properties();

		if (previousJobExecution != null) {
			JobParameters previousJobParameters = previousJobExecution.getJobParameters();

			if (previousJobParameters != null && !previousJobParameters.isEmpty()) {
				jobRestartProperties.putAll(previousJobParameters.toProperties());
			}
		}

		if (params != null) {
			Enumeration<?> propertyNames = params.propertyNames();

			while(propertyNames.hasMoreElements()) {
				String curName = (String) propertyNames.nextElement();
				jobRestartProperties.setProperty(curName, params.getProperty(curName));
			}
		}

		return jobRestartProperties;
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
	public long start(String jobName, Properties params) throws JobStartException,
	JobSecurityException {
		final JsrXmlApplicationContext batchContext = new JsrXmlApplicationContext(params);
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

		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.jsr.JsrJobContextFactoryBean").getBeanDefinition();
		beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
		batchContext.registerBeanDefinition(JSR_JOB_CONTEXT_BEAN_NAME, beanDefinition);

		if(baseContext != null) {
			batchContext.setParent(baseContext);
		} else {
			batchContext.getBeanFactory().registerSingleton("jobExplorer", jobExplorer);
			batchContext.getBeanFactory().registerSingleton("jobRepository", jobRepository);
			batchContext.getBeanFactory().registerSingleton("jobParametersConverter", jobParametersConverter);
			batchContext.getBeanFactory().registerSingleton("transactionManager", transactionManager);
		}

		try {
			batchContext.refresh();
		} catch (BeanCreationException e) {
			throw new JobStartException(e);
		}

		Assert.notNull(jobName, "The job name must not be null.");

		final org.springframework.batch.core.JobExecution jobExecution;

		try {
			JobParameters jobParameters = jobParametersConverter.getJobParameters(params);
			String [] jobNames = batchContext.getBeanNamesForType(Job.class);

			if(jobNames == null || jobNames.length <= 0) {
				throw new BatchRuntimeException("No Job defined in current context");
			}

			org.springframework.batch.core.JobInstance jobInstance = jobRepository.createJobInstance(jobNames[0], jobParameters);
			jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, jobConfigurationLocation);
		} catch (Exception e) {
			throw new JobStartException(e);
		}

		try {
			final Semaphore semaphore = new Semaphore(1);
			final List<Exception> exceptionHolder = Collections.synchronizedList(new ArrayList<>());
			semaphore.acquire();

			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					JsrJobContextFactoryBean factoryBean = null;
					try {
						factoryBean = (JsrJobContextFactoryBean) batchContext.getBean("&" + JSR_JOB_CONTEXT_BEAN_NAME);
						factoryBean.setJobExecution(jobExecution);
						final Job job = batchContext.getBean(Job.class);
						semaphore.release();
						// Initialization of the JobExecution for job level dependencies
						jobRegistry.register(job, jobExecution);
						job.execute(jobExecution);
						jobRegistry.remove(jobExecution);
					}
					catch (Exception e) {
						exceptionHolder.add(e);
					} finally {
						if(factoryBean != null) {
							factoryBean.close();
						}

						batchContext.close();

						if(semaphore.availablePermits() == 0) {
							semaphore.release();
						}
					}
				}
			});

			semaphore.acquire();
			if(exceptionHolder.size() > 0) {
				semaphore.release();
				throw new JobStartException(exceptionHolder.get(0));
			}
		}
		catch (Exception e) {
			if(jobRegistry.exists(jobExecution.getId())) {
				jobRegistry.remove(jobExecution);
			}
			jobExecution.upgradeStatus(BatchStatus.FAILED);
			if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
				jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}
			jobRepository.update(jobExecution);

			if(batchContext.isActive()) {
				batchContext.close();
			}

			throw new JobStartException(e);
		}
		return jobExecution.getId();
	}

	/**
	 * Stops the running job execution if it is currently running.
	 *
	 * @param executionId the database id for the {@link JobExecution} to be stopped.
	 * @throws NoSuchJobExecutionException thrown if {@link JobExecution} instance does not exist.
	 * @throws JobExecutionNotRunningException thrown if {@link JobExecution} is not running.
	 */
	@Override
	public void stop(long executionId) throws NoSuchJobExecutionException,
	JobExecutionNotRunningException, JobSecurityException {
		org.springframework.batch.core.JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		// Indicate the execution should be stopped by setting it's status to
		// 'STOPPING'. It is assumed that
		// the step implementation will check this status at chunk boundaries.
		BatchStatus status = jobExecution.getStatus();
		if (!(status == BatchStatus.STARTED || status == BatchStatus.STARTING)) {
			throw new JobExecutionNotRunningException("JobExecution must be running so that it can be stopped: "+jobExecution);
		}
		jobExecution.setStatus(BatchStatus.STOPPING);
		jobRepository.update(jobExecution);

		try {
			Job job = jobRegistry.getJob(jobExecution.getId());
			if (job instanceof StepLocator) {//can only process as StepLocator is the only way to get the step object
				//get the current stepExecution
				for (org.springframework.batch.core.StepExecution stepExecution : jobExecution.getStepExecutions()) {
					if (stepExecution.getStatus().isRunning()) {
						try {
							//have the step execution that's running -> need to 'stop' it
							Step step = ((StepLocator)job).getStep(stepExecution.getStepName());
							if (step instanceof TaskletStep) {
								Tasklet tasklet = ((TaskletStep)step).getTasklet();
								if (tasklet instanceof StoppableTasklet) {
									StepSynchronizationManager.register(stepExecution);
									((StoppableTasklet)tasklet).stop();
									StepSynchronizationManager.release();
								}
							}
						}
						catch (NoSuchStepException e) {
							logger.warn("Step not found",e);
						}
					}
				}
			}
		}
		catch (NoSuchJobException e) {
			logger.warn("Cannot find Job object",e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		baseContext = applicationContext;
	}

	private static class ExecutingJobRegistry {

		private Map<Long, Job> registry = new ConcurrentHashMap<>();

		public void register(Job job, org.springframework.batch.core.JobExecution jobExecution) throws DuplicateJobException {

			if(registry.containsKey(jobExecution.getId())) {
				throw new DuplicateJobException("This job execution has already been registered");
			} else {
				registry.put(jobExecution.getId(), job);
			}
		}

		public void remove(org.springframework.batch.core.JobExecution jobExecution) {
			if(!registry.containsKey(jobExecution.getId())) {
				throw new NoSuchJobExecutionException("The job execution " + jobExecution.getId() + " was not found");
			} else {
				registry.remove(jobExecution.getId());
			}
		}

		public boolean exists(long jobExecutionId) {
			return registry.containsKey(jobExecutionId);
		}

		public Job getJob(long jobExecutionId) {
			if(!registry.containsKey(jobExecutionId)) {
				throw new NoSuchJobExecutionException("The job execution " + jobExecutionId + " was not found");
			} else {
				return registry.get(jobExecutionId);
			}
		}
	}

	/**
	 * A singleton holder used to lazily bootstrap the base context used in JSR-352.
	 */
	protected static class BaseContextHolder {

		private ApplicationContext context;

		private static BaseContextHolder instance;

		private BaseContextHolder() {
			synchronized (BaseContextHolder.class) {
				if(this.context == null) {
					String overrideContextLocation = System.getProperty("JSR-352-BASE-CONTEXT");

					List<String> contextLocations = new ArrayList<>();

					contextLocations.add("jsrBaseContext.xml");

					if(overrideContextLocation != null) {
						contextLocations.add(overrideContextLocation);
					}

					this.context = new GenericXmlApplicationContext(
							contextLocations.toArray(new String[contextLocations.size()]));
				}
			}
		}

		public static BaseContextHolder getInstance() {
			synchronized (BaseContextHolder.class) {
				if(instance == null) {
					instance = new BaseContextHolder();
				}
			}

			return instance;
		}

		public ApplicationContext getContext() {
			return this.context;
		}
	}
}
