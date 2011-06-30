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

package org.springframework.batch.core.configuration.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.job.flow.FlowStep;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.PartitionStep;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.SimpleStepExecutionSplitter;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.partition.support.TaskExecutorPartitionHandler;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.batch.core.step.item.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.item.KeyGenerator;
import org.springframework.batch.core.step.item.SimpleStepFactoryBean;
import org.springframework.batch.core.step.job.JobParametersExtractor;
import org.springframework.batch.core.step.job.JobStep;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.Assert;

/**
 * This {@link FactoryBean} is used by the batch namespace parser to create
 * {@link Step} objects. Stores all of the properties that are configurable on
 * the &lt;step/&gt; (and its inner &lt;tasklet/&gt;). Based on which properties
 * are configured, the {@link #getObject()} method will delegate to the
 * appropriate class for generating the {@link Step}.
 * 
 * @author Dan Garrette
 * @author Josh Long
 * @see SimpleStepFactoryBean
 * @see FaultTolerantStepFactoryBean
 * @see TaskletStep
 * @since 2.0
 */
class StepParserStepFactoryBean<I, O> implements FactoryBean, BeanNameAware {

	private static final Log logger = LogFactory.getLog(StepParserStepFactoryBean.class);

	//
	// Step Attributes
	//
	private String name;

	//
	// Tasklet Attributes
	//
	private Boolean allowStartIfComplete;

	private JobRepository jobRepository;

	private Integer startLimit;

	private Tasklet tasklet;

	private PlatformTransactionManager transactionManager;

	//
	// Flow Elements
	//
	private Flow flow;

	//
	// Job Elements
	//
	private Job job;

	private JobLauncher jobLauncher;

	private JobParametersExtractor jobParametersExtractor;

	//
	// Partition Elements
	//
	private Partitioner partitioner;

	private static final int DEFAULT_GRID_SIZE = 6;

	private Step step;

	private PartitionHandler partitionHandler;

	private int gridSize = DEFAULT_GRID_SIZE;

	//
	// Tasklet Elements
	//
	private StepListener[] listeners;

	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses;

	private Integer transactionTimeout;

	private Propagation propagation;

	private Isolation isolation;

	//
	// Chunk Attributes
	//
	private Integer cacheCapacity;

	private CompletionPolicy chunkCompletionPolicy;

	private Integer commitInterval;

	private Boolean readerTransactionalQueue;

	private Boolean processorTransactional;

	private Integer retryLimit;

	private BackOffPolicy backOffPolicy;

	private RetryPolicy retryPolicy;

	private RetryContextCache retryContextCache;

	private KeyGenerator keyGenerator;

	private Integer skipLimit;

	private SkipPolicy skipPolicy;

	private TaskExecutor taskExecutor;

	private Integer throttleLimit;

	private ItemReader<? extends I> itemReader;

	private ItemProcessor<? super I, ? extends O> itemProcessor;

	private ItemWriter<? super O> itemWriter;

	//
	// Chunk Elements
	//
	private RetryListener[] retryListeners;

	private Map<Class<? extends Throwable>, Boolean> skippableExceptionClasses;

	private Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses;

	private ItemStream[] streams;

	//
	// Additional
	//
	private boolean hasChunkElement = false;

	private StepExecutionAggregator stepExecutionAggregator;

	/**
	 * Create a {@link Step} from the configuration provided.
	 * 
	 * @see FactoryBean#getObject()
	 */
	public final Object getObject() throws Exception {
		if (hasChunkElement) {
			Assert.isNull(tasklet, "Step [" + name
					+ "] has both a <chunk/> element and a 'ref' attribute  referencing a Tasklet.");

			validateFaultTolerantSettings();
			if (isFaultTolerant()) {
				FaultTolerantStepFactoryBean<I, O> fb = new FaultTolerantStepFactoryBean<I, O>();
				configureSimple(fb);
				configureFaultTolerant(fb);
				return fb.getObject();
			}
			else {
				SimpleStepFactoryBean<I, O> fb = new SimpleStepFactoryBean<I, O>();
				configureSimple(fb);
				return fb.getObject();
			}
		}
		else if (tasklet != null) {
			TaskletStep ts = new TaskletStep();
			configureTaskletStep(ts);
			return ts;
		}
		else if (flow != null) {
			FlowStep ts = new FlowStep();
			configureFlowStep(ts);
			return ts;
		}
		else if (job != null) {
			JobStep ts = new JobStep();
			configureJobStep(ts);
			return ts;
		}
		else {
			PartitionStep ts = new PartitionStep();
			configurePartitionStep(ts);
			return ts;
		}
	}

	public boolean requiresTransactionManager() {
		// Currently all step implementations other than TaskletStep are
		// AbstractStep and do not require a transaction manager
		return hasChunkElement || tasklet != null;
	}

	private void configureAbstractStep(AbstractStep ts) {
		if (name != null) {
			ts.setName(name);
		}
		if (allowStartIfComplete != null) {
			ts.setAllowStartIfComplete(allowStartIfComplete);
		}
		if (jobRepository != null) {
			ts.setJobRepository(jobRepository);
		}
		if (startLimit != null) {
			ts.setStartLimit(startLimit);
		}
		if (listeners != null) {
			List<StepExecutionListener> newListeners = new ArrayList<StepExecutionListener>();
			for (StepListener listener : listeners) {
				if (listener instanceof StepExecutionListener) {
					newListeners.add((StepExecutionListener) listener);
				}
			}
			ts.setStepExecutionListeners(newListeners.toArray(new StepExecutionListener[0]));
		}
	}

	private void configurePartitionStep(PartitionStep ts) {
		Assert.state(partitioner != null, "A Partitioner must be provided for a partition step");
		configureAbstractStep(ts);

		if (partitionHandler != null) {
			ts.setPartitionHandler(partitionHandler);
		}
		else {
			TaskExecutorPartitionHandler partitionHandler = new TaskExecutorPartitionHandler();
			partitionHandler.setStep(step);
			if (taskExecutor == null) {
				taskExecutor = new SyncTaskExecutor();
			}
			partitionHandler.setGridSize(gridSize);
			partitionHandler.setTaskExecutor(taskExecutor);
			ts.setPartitionHandler(partitionHandler);
		}

		boolean allowStartIfComplete = this.allowStartIfComplete != null ? this.allowStartIfComplete : false;
		String name = this.name;
		if (step != null) {
			try {
				allowStartIfComplete = step.isAllowStartIfComplete();
				name = step.getName();
			}
			catch (Exception e) {
				logger.info("Ignored exception from step asking for name and allowStartIfComplete flag. "
						+ "Using default from enclosing PartitionStep (" + name + "," + allowStartIfComplete + ").");
			}
		}
		SimpleStepExecutionSplitter splitter = new SimpleStepExecutionSplitter(jobRepository, allowStartIfComplete,
				name, partitioner);
		ts.setStepExecutionSplitter(splitter);
		if (stepExecutionAggregator != null) {
			ts.setStepExecutionAggregator(stepExecutionAggregator);
		}
	}

	private Object extractTarget(Object target, Class<?> type) {
		if (target instanceof Advised) {
			Object source;
			try {
				source = ((Advised) target).getTargetSource().getTarget();
			}
			catch (Exception e) {
				throw new IllegalStateException("Could not extract target from proxy", e);
			}
			if (source instanceof Advised) {
				source = extractTarget(source, type);
			}
			if (type.isAssignableFrom(source.getClass())) {
				target = source;
			}
		}
		return target;
	}

	private void configureSimple(SimpleStepFactoryBean<I, O> fb) {
		if (name != null) {
			fb.setBeanName(name);
		}
		if (allowStartIfComplete != null) {
			fb.setAllowStartIfComplete(allowStartIfComplete);
		}
		if (jobRepository != null) {
			fb.setJobRepository(jobRepository);
		}
		if (startLimit != null) {
			fb.setStartLimit(startLimit);
		}
		if (transactionManager != null) {
			fb.setTransactionManager(transactionManager);
		}
		if (listeners != null) {
			fb.setListeners(listeners);
		}
		if (transactionTimeout != null) {
			fb.setTransactionTimeout(transactionTimeout);
		}
		if (propagation != null) {
			fb.setPropagation(propagation);
		}
		if (isolation != null) {
			fb.setIsolation(isolation);
		}

		if (chunkCompletionPolicy != null) {
			fb.setChunkCompletionPolicy(chunkCompletionPolicy);
		}
		if (commitInterval != null) {
			fb.setCommitInterval(commitInterval);
		}
		if (taskExecutor != null) {
			fb.setTaskExecutor(taskExecutor);
		}
		if (throttleLimit != null) {
			fb.setThrottleLimit(throttleLimit);
		}
		if (itemReader != null) {
			fb.setItemReader(itemReader);
		}
		if (itemProcessor != null) {
			fb.setItemProcessor(itemProcessor);
		}
		if (itemWriter != null) {
			fb.setItemWriter(itemWriter);
		}

		if (streams != null) {
			fb.setStreams(streams);
		}
	}

	private void configureFaultTolerant(FaultTolerantStepFactoryBean<I, O> fb) {
		if (cacheCapacity != null) {
			fb.setCacheCapacity(cacheCapacity);
		}
		if (readerTransactionalQueue != null) {
			fb.setIsReaderTransactionalQueue(readerTransactionalQueue);
		}
		if (processorTransactional != null) {
			fb.setProcessorTransactional(processorTransactional);
		}
		if (retryLimit != null) {
			fb.setRetryLimit(retryLimit);
		}
		if (skipLimit != null) {
			fb.setSkipLimit(skipLimit);
		}
		if (skipPolicy != null) {
			fb.setSkipPolicy(skipPolicy);
		}
		if (backOffPolicy != null) {
			fb.setBackOffPolicy(backOffPolicy);
		}
		if (retryPolicy != null) {
			fb.setRetryPolicy(retryPolicy);
		}
		if (retryContextCache != null) {
			fb.setRetryContextCache(retryContextCache);
		}
		if (keyGenerator != null) {
			fb.setKeyGenerator(keyGenerator);
		}

		if (retryListeners != null) {
			fb.setRetryListeners(retryListeners);
		}
		if (skippableExceptionClasses != null) {
			fb.setSkippableExceptionClasses(skippableExceptionClasses);
		}
		if (retryableExceptionClasses != null) {
			fb.setRetryableExceptionClasses(retryableExceptionClasses);
		}
		if (noRollbackExceptionClasses != null) {
			fb.setNoRollbackExceptionClasses(noRollbackExceptionClasses);
		}
	}

	@SuppressWarnings("serial")
	private void configureTaskletStep(TaskletStep ts) {
		configureAbstractStep(ts);
		if (listeners != null) {
			List<ChunkListener> newListeners = new ArrayList<ChunkListener>();
			for (StepListener listener : listeners) {
				if (listener instanceof ChunkListener) {
					newListeners.add((ChunkListener) listener);
				}
			}
			ts.setChunkListeners(newListeners.toArray(new ChunkListener[0]));
		}
		if (tasklet != null) {
			ts.setTasklet(tasklet);
		}
		if (taskExecutor != null) {
			TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
			repeatTemplate.setTaskExecutor(taskExecutor);
			if (throttleLimit != null) {
				repeatTemplate.setThrottleLimit(throttleLimit);
			}
			ts.setStepOperations(repeatTemplate);
		}
		if (transactionManager != null) {
			ts.setTransactionManager(transactionManager);
		}
		if (transactionTimeout != null || propagation != null || isolation != null
				|| noRollbackExceptionClasses != null) {
			DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
			if (propagation != null) {
				attribute.setPropagationBehavior(propagation.value());
			}
			if (isolation != null) {
				attribute.setIsolationLevel(isolation.value());
			}
			if (transactionTimeout != null) {
				attribute.setTimeout(transactionTimeout);
			}
			Collection<Class<? extends Throwable>> exceptions = noRollbackExceptionClasses == null ? new HashSet<Class<? extends Throwable>>()
					: noRollbackExceptionClasses;
			final BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(exceptions, false);
			ts.setTransactionAttribute(new DefaultTransactionAttribute(attribute) {
				@Override
				public boolean rollbackOn(Throwable ex) {
					return classifier.classify(ex);
				}
			});
		}
	}

	@SuppressWarnings("serial")
	private void configureFlowStep(FlowStep ts) {
		configureAbstractStep(ts);
		if (flow != null) {
			ts.setFlow(flow);
		}
	}

	@SuppressWarnings("serial")
	private void configureJobStep(JobStep ts) throws Exception {
		configureAbstractStep(ts);
		if (job != null) {
			ts.setJob(job);
		}
		if (jobParametersExtractor != null) {
			ts.setJobParametersExtractor(jobParametersExtractor);
		}
		if (jobLauncher == null) {
			SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
			jobLauncher.setJobRepository(jobRepository);
			jobLauncher.afterPropertiesSet();
			this.jobLauncher = jobLauncher;
		}
		ts.setJobLauncher(jobLauncher);
	}

	private void validateFaultTolerantSettings() {
		validateDependency("skippable-exception-classes", skippableExceptionClasses, "skip-limit", skipLimit, true);
		validateDependency("retryable-exception-classes", retryableExceptionClasses, "retry-limit", retryLimit, true);
		validateDependency("retry-listeners", retryListeners, "retry-limit", retryLimit, false);
		if (isPresent(processorTransactional) && !processorTransactional && isPresent(readerTransactionalQueue)
				&& readerTransactionalQueue) {
			throw new IllegalArgumentException(
					"The field 'processor-transactional' cannot be false if 'reader-transactional-queue' is true");
		}
	}

	/**
	 * Check if a field is present then a second is also. If the
	 * twoWayDependency flag is set then the opposite must also be true: if the
	 * second value is present, the first must also be.
	 * 
	 * @param dependentName the name of the first field
	 * @param dependentValue the value of the first field
	 * @param name the name of the other field (which should be absent if the
	 * first is present)
	 * @param value the value of the other field
	 * @param twoWayDependency true if both depend on each other
	 * @throws IllegalArgumentException if either condition is violated
	 */
	private void validateDependency(String dependentName, Object dependentValue, String name, Object value,
			boolean twoWayDependency) {
		if (isPresent(dependentValue) && !isPresent(value)) {
			throw new IllegalArgumentException("The field '" + dependentName + "' is not permitted on the step ["
					+ this.name + "] because there is no '" + name + "'.");
		}
		if (twoWayDependency && isPresent(value) && !isPresent(dependentValue)) {
			throw new IllegalArgumentException("The field '" + name + "' is not permitted on the step [" + this.name
					+ "] because there is no '" + dependentName + "'.");
		}
	}

	/**
	 * Is the object non-null (or if an Integer, non-zero)?
	 * 
	 * @param o an object
	 * @return true if the object has a value
	 */
	private boolean isPresent(Object o) {
		if (o instanceof Integer) {
			return isPositive((Integer) o);
		}
		return o != null;
	}

	private boolean isFaultTolerant() {
		return backOffPolicy != null || skipPolicy != null || retryPolicy != null || isPositive(skipLimit)
				|| isPositive(retryLimit) || isPositive(cacheCapacity) || isTrue(readerTransactionalQueue);
	}

	private boolean isTrue(Boolean b) {
		return b != null && b.booleanValue();
	}

	private boolean isPositive(Integer n) {
		return n != null && n > 0;
	}

	public Class<TaskletStep> getObjectType() {
		return TaskletStep.class;
	}

	public boolean isSingleton() {
		return true;
	}

	// =========================================================
	// Step Attributes
	// =========================================================

	/**
	 * Set the bean name property, which will become the name of the
	 * {@link Step} when it is created.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	public void setBeanName(String name) {
		if (this.name == null) {
			this.name = name;
		}
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	// =========================================================
	// Flow Attributes
	// =========================================================

	/**
	 * @param flow the flow to set
	 */
	public void setFlow(Flow flow) {
		this.flow = flow;
	}

	// =========================================================
	// Job Attributes
	// =========================================================

	public void setJob(Job job) {
		this.job = job;
	}

	public void setJobParametersExtractor(JobParametersExtractor jobParametersExtractor) {
		this.jobParametersExtractor = jobParametersExtractor;
	}

	public void setJobLauncher(JobLauncher jobLauncher) {
		this.jobLauncher = jobLauncher;
	}

	// =========================================================
	// Partition Attributes
	// =========================================================

	/**
	 * @param partitioner the partitioner to set
	 */
	public void setPartitioner(Partitioner partitioner) {
		this.partitioner = partitioner;
	}

	/**
	 * @param stepExecutionAggregator the stepExecutionAggregator to set
	 */
	public void setStepExecutionAggregator(StepExecutionAggregator stepExecutionAggregator) {
		this.stepExecutionAggregator = stepExecutionAggregator;
	}

	/**
	 * @param partitionHandler the partitionHandler to set
	 */
	public void setPartitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
	}

	/**
	 * @param gridSize the gridSize to set
	 */
	public void setGridSize(int gridSize) {
		this.gridSize = gridSize;
	}

	/**
	 * @param step the step to set
	 */
	public void setStep(Step step) {
		this.step = step;
	}

	// =========================================================
	// Tasklet Attributes
	// =========================================================

	/**
	 * Public setter for the flag to indicate that the step should be replayed
	 * on a restart, even if successful the first time.
	 * 
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;

	}

	/**
	 * @return jobRepository
	 */
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * 
	 * @param jobRepository
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * The number of times that the step should be allowed to start
	 * 
	 * @param startLimit
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/**
	 * A preconfigured {@link Tasklet} to use.
	 * 
	 * @param tasklet
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	/**
	 * @return transactionManager
	 */
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	/**
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	// =========================================================
	// Tasklet Elements
	// =========================================================

	/**
	 * The listeners to inject into the {@link Step}. Any instance of
	 * {@link StepListener} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param listeners an array of listeners
	 */
	public void setListeners(StepListener[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * Exception classes that may not cause a rollback if encountered in the
	 * right place.
	 * 
	 * @param noRollbackExceptionClasses the noRollbackExceptionClasses to set
	 */
	public void setNoRollbackExceptionClasses(Collection<Class<? extends Throwable>> noRollbackExceptionClasses) {
		this.noRollbackExceptionClasses = noRollbackExceptionClasses;
	}

	/**
	 * @param transactionTimeout the transactionTimeout to set
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * @param isolation the isolation to set
	 */
	public void setIsolation(Isolation isolation) {
		this.isolation = isolation;
	}

	/**
	 * @param propagation the propagation to set
	 */
	public void setPropagation(Propagation propagation) {
		this.propagation = propagation;
	}

	// =========================================================
	// Parent Attributes - can be provided in parent bean but not namespace
	// =========================================================

	/**
	 * A backoff policy to be applied to retry process.
	 * 
	 * @param backOffPolicy the {@link BackOffPolicy} to set
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * A retry policy to apply when exceptions occur. If this is specified then
	 * the retry limit and retryable exceptions will be ignored.
	 * 
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * @param retryContextCache the {@link RetryContextCache} to set
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * A key generator that can be used to compare items with previously
	 * recorded items in a retry. Only used if the reader is a transactional
	 * queue.
	 * 
	 * @param keyGenerator the {@link KeyGenerator} to set
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	// =========================================================
	// Chunk Attributes
	// =========================================================

	/**
	 * Public setter for the capacity of the cache in the retry policy. If more
	 * items than this fail without being skipped or recovered an exception will
	 * be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems.<br/>
	 * <p/>
	 * The default value should be high enough and more for most purposes. To
	 * breach the limit in a single-threaded step typically you have to have
	 * this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.<br/>
	 * 
	 * @param cacheCapacity the cache capacity to set (greater than 0 else
	 * ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Public setter for the {@link CompletionPolicy} applying to the chunk
	 * level. A transaction will be committed when this policy decides to
	 * complete. Defaults to a {@link SimpleCompletionPolicy} with chunk size
	 * equal to the commitInterval property.
	 * 
	 * @param chunkCompletionPolicy the chunkCompletionPolicy to set
	 */
	public void setChunkCompletionPolicy(CompletionPolicy chunkCompletionPolicy) {
		this.chunkCompletionPolicy = chunkCompletionPolicy;
	}

	/**
	 * Set the commit interval. Either set this or the chunkCompletionPolicy but
	 * not both.
	 * 
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * Flag to signal that the reader is transactional (usually a JMS consumer)
	 * so that items are re-presented after a rollback. The default is false and
	 * readers are assumed to be forward-only.
	 * 
	 * @param isReaderTransactionalQueue the value of the flag
	 */
	public void setIsReaderTransactionalQueue(boolean isReaderTransactionalQueue) {
		this.readerTransactionalQueue = isReaderTransactionalQueue;
	}

	/**
	 * Flag to signal that the processor is transactional, in which case it
	 * should be called for every item in every transaction. If false then we
	 * can cache the processor results between transactions in the case of a
	 * rollback.
	 * 
	 * @param processorTransactional the value to set
	 */
	public void setProcessorTransactional(Boolean processorTransactional) {
		this.processorTransactional = processorTransactional;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this
	 * limit. Note this limit includes the initial attempt to process the item,
	 * therefore <code>retryLimit == 1</code> by default.
	 * 
	 * @param retryLimit the retry limit to set, must be greater or equal to 1.
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for a limit that determines skip policy. If this value is
	 * positive then an exception in chunk processing will cause the item to be
	 * skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the
	 * step to abort.
	 * 
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for a skip policy. If this value is set then the skip limit
	 * and skippable exceptions are ignored.
	 * 
	 * @param skipPolicy the {@link SkipPolicy} to set
	 */
	public void setSkipPolicy(SkipPolicy skipPolicy) {
		this.skipPolicy = skipPolicy;
	}

	/**
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will
	 * be used to execute the chunk processing inside the {@link Step}.
	 * 
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of tasks
	 * queued for concurrent processing to prevent thread pools from being
	 * overwhelmed. Defaults to
	 * {@link TaskExecutorRepeatTemplate#DEFAULT_THROTTLE_LIMIT}.
	 * 
	 * @param throttleLimit the throttle limit to set.
	 */
	public void setThrottleLimit(Integer throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	/**
	 * @param itemReader the {@link ItemReader} to set
	 */
	public void setItemReader(ItemReader<? extends I> itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemProcessor the {@link ItemProcessor} to set
	 */
	public void setItemProcessor(ItemProcessor<? super I, ? extends O> itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/**
	 * @param itemWriter the {@link ItemWriter} to set
	 */
	public void setItemWriter(ItemWriter<? super O> itemWriter) {
		this.itemWriter = itemWriter;
	}

	// =========================================================
	// Chunk Elements
	// =========================================================

	/**
	 * Public setter for the {@link RetryListener}s.
	 * 
	 * @param retryListeners the {@link RetryListener}s to set
	 */
	public void setRetryListeners(RetryListener... retryListeners) {
		this.retryListeners = retryListeners;
	}

	/**
	 * Public setter for exception classes that when raised won't crash the job
	 * but will result in transaction rollback and the item which handling
	 * caused the exception will be skipped.
	 * 
	 * @param exceptionClasses
	 */
	public void setSkippableExceptionClasses(Map<Class<? extends Throwable>, Boolean> exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Public setter for exception classes that will retry the item when raised.
	 * 
	 * @param retryableExceptionClasses the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * The streams to inject into the {@link Step}. Any instance of
	 * {@link ItemStream} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param streams an array of listeners
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams = streams;
	}

	// =========================================================
	// Additional
	// =========================================================

	/**
	 * @param hasChunkElement
	 */
	public void setHasChunkElement(boolean hasChunkElement) {
		this.hasChunkElement = hasChunkElement;
	}

}
