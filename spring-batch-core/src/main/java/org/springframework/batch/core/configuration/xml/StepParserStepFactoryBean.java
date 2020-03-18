/*
 * Copyright 2006-2013 the original author or authors.
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

package org.springframework.batch.core.configuration.xml;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import javax.batch.api.chunk.listener.RetryProcessListener;
import javax.batch.api.chunk.listener.RetryReadListener;
import javax.batch.api.chunk.listener.RetryWriteListener;
import javax.batch.api.chunk.listener.SkipProcessListener;
import javax.batch.api.chunk.listener.SkipReadListener;
import javax.batch.api.chunk.listener.SkipWriteListener;
import javax.batch.api.partition.PartitionCollector;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.jsr.ChunkListenerAdapter;
import org.springframework.batch.core.jsr.ItemProcessListenerAdapter;
import org.springframework.batch.core.jsr.ItemReadListenerAdapter;
import org.springframework.batch.core.jsr.ItemWriteListenerAdapter;
import org.springframework.batch.core.jsr.RetryProcessListenerAdapter;
import org.springframework.batch.core.jsr.RetryReadListenerAdapter;
import org.springframework.batch.core.jsr.RetryWriteListenerAdapter;
import org.springframework.batch.core.jsr.SkipListenerAdapter;
import org.springframework.batch.core.jsr.StepListenerAdapter;
import org.springframework.batch.core.jsr.partition.PartitionCollectorAdapter;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.partition.support.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.AbstractTaskletStepBuilder;
import org.springframework.batch.core.step.builder.FaultTolerantStepBuilder;
import org.springframework.batch.core.step.builder.FlowStepBuilder;
import org.springframework.batch.core.step.builder.JobStepBuilder;
import org.springframework.batch.core.step.builder.PartitionStepBuilder;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.builder.StepBuilderHelper;
import org.springframework.batch.core.step.builder.TaskletStepBuilder;
import org.springframework.batch.core.step.factory.FaultTolerantStepFactoryBean;
import org.springframework.batch.core.step.factory.SimpleStepFactoryBean;
import org.springframework.batch.core.step.item.KeyGenerator;
import org.springframework.batch.core.step.job.JobParametersExtractor;
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
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.policy.MapRetryContextCache;
import org.springframework.retry.policy.RetryContextCache;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.util.Assert;

/**
 * This {@link FactoryBean} is used by the batch namespace parser to create {@link Step} objects. Stores all of the
 * properties that are configurable on the &lt;step/&gt; (and its inner &lt;tasklet/&gt;). Based on which properties are
 * configured, the {@link #getObject()} method will delegate to the appropriate class for generating the {@link Step}.
 *
 * @author Dan Garrette
 * @author Josh Long
 * @author Michael Minella
 * @author Chris Schaefer
 * @see SimpleStepFactoryBean
 * @see FaultTolerantStepFactoryBean
 * @see TaskletStep
 * @since 2.0
 */
public class StepParserStepFactoryBean<I, O> implements FactoryBean<Step>, BeanNameAware {

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

	private Set<Object> stepExecutionListeners = new LinkedHashSet<>();

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

	private Queue<Serializable> partitionQueue;

	private ReentrantLock partitionLock;

	//
	// Tasklet Elements
	//
	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses;

	private Integer transactionTimeout;

	private Propagation propagation;

	private Isolation isolation;

	private Set<ChunkListener> chunkListeners = new LinkedHashSet<>();

	//
	// Chunk Attributes
	//
	private int cacheCapacity = 0;

	private CompletionPolicy chunkCompletionPolicy;

	private Integer commitInterval;

	private Boolean readerTransactionalQueue;

	private Boolean processorTransactional;

	private int retryLimit = 0;

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

	private Map<Class<? extends Throwable>, Boolean> skippableExceptionClasses = new HashMap<>();

	private Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses = new HashMap<>();

	private ItemStream[] streams;

	private Set<ItemReadListener<I>> readListeners = new LinkedHashSet<>();

	private Set<ItemWriteListener<O>> writeListeners = new LinkedHashSet<>();

	private Set<ItemProcessListener<I, O>> processListeners = new LinkedHashSet<>();

	private Set<SkipListener<I, O>> skipListeners = new LinkedHashSet<>();

	private Set<org.springframework.batch.core.jsr.RetryListener> jsrRetryListeners = new LinkedHashSet<>();

	//
	// Additional
	//
	private boolean hasChunkElement = false;

	private StepExecutionAggregator stepExecutionAggregator;

	/**
	 * @param queue The {@link Queue} that is used for communication between {@link javax.batch.api.partition.PartitionCollector} and {@link javax.batch.api.partition.PartitionAnalyzer}
	 */
	public void setPartitionQueue(Queue<Serializable> queue) {
		this.partitionQueue = queue;
	}

	/**
	 * Used to coordinate access to the partition queue between the {@link javax.batch.api.partition.PartitionCollector} and {@link javax.batch.api.partition.AbstractPartitionAnalyzer}
	 *
	 * @param lock a lock that will be locked around accessing the partition queue
	 */
	public void setPartitionLock(ReentrantLock lock) {
		this.partitionLock = lock;
	}

	/**
	 * Create a {@link Step} from the configuration provided.
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public Step getObject() throws Exception {
		if (hasChunkElement) {
			Assert.isNull(tasklet, "Step [" + name
					+ "] has both a <chunk/> element and a 'ref' attribute  referencing a Tasklet.");

			validateFaultTolerantSettings();

			if (isFaultTolerant()) {
				return createFaultTolerantStep();
			}
			else {
				return createSimpleStep();
			}
		}
		else if (tasklet != null) {
			return createTaskletStep();
		}
		else if (flow != null) {
			return createFlowStep();
		}
		else if (job != null) {
			return createJobStep();
		}
		else {
			return createPartitionStep();
		}
	}

	public boolean requiresTransactionManager() {
		// Currently all step implementations other than TaskletStep are
		// AbstractStep and do not require a transaction manager
		return hasChunkElement || tasklet != null;
	}

	/**
	 * @param builder {@link StepBuilderHelper} representing the step to be enhanced
	 */
	protected void enhanceCommonStep(StepBuilderHelper<?> builder) {
		if (allowStartIfComplete != null) {
			builder.allowStartIfComplete(allowStartIfComplete);
		}
		if (startLimit != null) {
			builder.startLimit(startLimit);
		}
		builder.repository(jobRepository);
		builder.transactionManager(transactionManager);
		for (Object listener : stepExecutionListeners) {
			if(listener instanceof StepExecutionListener) {
				builder.listener((StepExecutionListener) listener);
			} else if(listener instanceof javax.batch.api.listener.StepListener) {
				builder.listener(new StepListenerAdapter((javax.batch.api.listener.StepListener) listener));
			}
		}
	}

	protected Step createPartitionStep() {

		PartitionStepBuilder builder;
		if (partitioner != null) {
			builder = new StepBuilder(name).partitioner(step != null ? step.getName() : name, partitioner).step(step);
		}
		else {
			builder = new StepBuilder(name).partitioner(step);
		}
		enhanceCommonStep(builder);

		if (partitionHandler != null) {
			builder.partitionHandler(partitionHandler);
		}
		else {
			builder.gridSize(gridSize);
			builder.taskExecutor(taskExecutor);
		}

		builder.aggregator(stepExecutionAggregator);

		return builder.build();

	}

	protected Step createFaultTolerantStep() {

		FaultTolerantStepBuilder<I, O> builder = getFaultTolerantStepBuilder(this.name);

		if (commitInterval != null) {
			builder.chunk(commitInterval);
		}
		builder.chunk(chunkCompletionPolicy);
		enhanceTaskletStepBuilder(builder);

		builder.reader(itemReader);
		builder.writer(itemWriter);
		builder.processor(itemProcessor);

		if (processorTransactional != null && !processorTransactional) {
			builder.processorNonTransactional();
		}

		if (readerTransactionalQueue!=null && readerTransactionalQueue==true) {
			builder.readerIsTransactionalQueue();
		}

		for (SkipListener<I, O> listener : skipListeners) {
			builder.listener(listener);
		}

		for (org.springframework.batch.core.jsr.RetryListener listener : jsrRetryListeners) {
			builder.listener(listener);
		}

		registerItemListeners(builder);

		if (skipPolicy != null) {
			builder.skipPolicy(skipPolicy);
		}
		else if (skipLimit!=null) {
			builder.skipLimit(skipLimit);
			for (Class<? extends Throwable> type : skippableExceptionClasses.keySet()) {
				if (skippableExceptionClasses.get(type)) {
					builder.skip(type);
				}
				else {
					builder.noSkip(type);
				}
			}
		}

		if (retryListeners != null) {
			for (RetryListener listener : retryListeners) {
				builder.listener(listener);
			}
		}

		if (retryContextCache == null && cacheCapacity > 0) {
			retryContextCache = new MapRetryContextCache(cacheCapacity);
		}
		builder.retryContextCache(retryContextCache);
		builder.keyGenerator(keyGenerator);
		if (retryPolicy != null) {
			builder.retryPolicy(retryPolicy);
		}
		else {
			builder.retryLimit(retryLimit);
			builder.backOffPolicy(backOffPolicy);
			for (Class<? extends Throwable> type : retryableExceptionClasses.keySet()) {
				if (retryableExceptionClasses.get(type)) {
					builder.retry(type);
				}
				else {
					builder.noRetry(type);
				}
			}
		}

		if (noRollbackExceptionClasses != null) {
			for (Class<? extends Throwable> type : noRollbackExceptionClasses) {
				builder.noRollback(type);
			}
		}

		return builder.build();

	}

	protected FaultTolerantStepBuilder<I, O> getFaultTolerantStepBuilder(String stepName) {
		return new FaultTolerantStepBuilder<>(new StepBuilder(stepName));
	}

	protected void registerItemListeners(SimpleStepBuilder<I, O> builder) {
		for (ItemReadListener<I> listener : readListeners) {
			builder.listener(listener);
		}
		for (ItemWriteListener<O> listener : writeListeners) {
			builder.listener(listener);
		}
		for (ItemProcessListener<I, O> listener : processListeners) {
			builder.listener(listener);
		}
	}

	protected Step createSimpleStep() {
		SimpleStepBuilder<I, O> builder = getSimpleStepBuilder(name);

		setChunk(builder);

		enhanceTaskletStepBuilder(builder);
		registerItemListeners(builder);
		builder.reader(itemReader);
		builder.writer(itemWriter);
		builder.processor(itemProcessor);
		return builder.build();
	}

	protected void setChunk(SimpleStepBuilder<I, O> builder) {
		if (commitInterval != null) {
			builder.chunk(commitInterval);
		}
		builder.chunk(chunkCompletionPolicy);
	}

	protected CompletionPolicy getCompletionPolicy() {
		return this.chunkCompletionPolicy;
	}

	protected SimpleStepBuilder<I, O> getSimpleStepBuilder(String stepName) {
		return new SimpleStepBuilder<>(new StepBuilder(stepName));
	}

	/**
	 * @return a new {@link TaskletStep}
	 */
	protected TaskletStep createTaskletStep() {
		TaskletStepBuilder builder = new StepBuilder(name).tasklet(tasklet);
		enhanceTaskletStepBuilder(builder);
		return builder.build();
	}

	@SuppressWarnings("serial")
	protected void enhanceTaskletStepBuilder(AbstractTaskletStepBuilder<?> builder) {

		enhanceCommonStep(builder);
		for (ChunkListener listener : chunkListeners) {
			if(listener instanceof PartitionCollectorAdapter) {
				((PartitionCollectorAdapter) listener).setPartitionLock(partitionLock);
			}

			builder.listener(listener);

		}
		builder.taskExecutor(taskExecutor);
		if (throttleLimit != null) {
			builder.throttleLimit(throttleLimit);
		}
		builder.transactionManager(transactionManager);
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
			Collection<Class<? extends Throwable>> exceptions = noRollbackExceptionClasses == null ? new HashSet<>()
					: noRollbackExceptionClasses;
			final BinaryExceptionClassifier classifier = new BinaryExceptionClassifier(exceptions, false);
			builder.transactionAttribute(new DefaultTransactionAttribute(attribute) {
				@Override
				public boolean rollbackOn(Throwable ex) {
					return classifier.classify(ex);
				}
			});
		}
		if (streams != null) {
			for (ItemStream stream : streams) {
				builder.stream(stream);
			}
		}

	}

	protected Step createFlowStep() {
		FlowStepBuilder builder = new StepBuilder(name).flow(flow);
		enhanceCommonStep(builder);
		return builder.build();
	}

	private Step createJobStep() throws Exception {

		JobStepBuilder builder = new StepBuilder(name).job(job);
		enhanceCommonStep(builder);
		builder.parametersExtractor(jobParametersExtractor);
		builder.launcher(jobLauncher);
		return builder.build();

	}

	/**
	 * Validates that all components required to build a fault tolerant step are set
	 */
	protected void validateFaultTolerantSettings() {
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
	 * Check if a field is present then a second is also. If the twoWayDependency flag is set then the opposite must
	 * also be true: if the second value is present, the first must also be.
	 *
	 * @param dependentName the name of the first field
	 * @param dependentValue the value of the first field
	 * @param name the name of the other field (which should be absent if the first is present)
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
		if (o instanceof Collection) {
			return !((Collection<?>) o).isEmpty();
		}
		if (o instanceof Map) {
			return !((Map<?, ?>) o).isEmpty();
		}
		return o != null;
	}

	/**
	 * @return true if the step is configured with any components that require fault tolerance
	 */
	protected boolean isFaultTolerant() {
		return backOffPolicy != null || skipPolicy != null || retryPolicy != null || isPositive(skipLimit)
				|| isPositive(retryLimit) || isPositive(cacheCapacity) || isTrue(readerTransactionalQueue);
	}

	private boolean isTrue(Boolean b) {
		return b != null && b.booleanValue();
	}

	private boolean isPositive(Integer n) {
		return n != null && n > 0;
	}

	@Override
	public Class<TaskletStep> getObjectType() {
		return TaskletStep.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	// =========================================================
	// Step Attributes
	// =========================================================

	/**
	 * Set the bean name property, which will become the name of the {@link Step} when it is created.
	 *
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	@Override
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

	public String getName() {
		return this.name;
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
	 * @return stepExecutionAggregator the current step's {@link StepExecutionAggregator}
	 */
	protected StepExecutionAggregator getStepExecutionAggergator() {
		return this.stepExecutionAggregator;
	}

	/**
	 * @param partitionHandler the partitionHandler to set
	 */
	public void setPartitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
	}

	/**
	 * @return partitionHandler the current step's {@link PartitionHandler}
	 */
	protected PartitionHandler getPartitionHandler() {
		return this.partitionHandler;
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
	 * Public setter for the flag to indicate that the step should be replayed on a restart, even if successful the
	 * first time.
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
	 * @param jobRepository {@link JobRepository} instance to be used by the step.
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * The number of times that the step should be allowed to start
	 *
	 * @param startLimit int containing the number of times a step should be allowed to start.
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/**
	 * A preconfigured {@link Tasklet} to use.
	 *
	 * @param tasklet {@link Tasklet} instance to be used by step.
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	protected Tasklet getTasklet() {
		return this.tasklet;
	}

	/**
	 * @return transactionManager instance of {@link PlatformTransactionManager}
	 * used by the step.
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
	 * The listeners to inject into the {@link Step}. Any instance of {@link StepListener} can be used, and will then
	 * receive callbacks at the appropriate stage in the step.
	 *
	 * @param listeners an array of listeners
	 */
	@SuppressWarnings("unchecked")
	public void setListeners(Object[] listeners) {
		for (Object listener : listeners) {
			if (listener instanceof SkipListener) {
				SkipListener<I, O> skipListener = (SkipListener<I, O>) listener;
				skipListeners.add(skipListener);
			}
			if(listener instanceof SkipReadListener) {
				SkipListener<I, O> skipListener = new SkipListenerAdapter<>((SkipReadListener) listener, null, null);
				skipListeners.add(skipListener);
			}
			if(listener instanceof SkipProcessListener) {
				SkipListener<I, O> skipListener = new SkipListenerAdapter<>(null, (SkipProcessListener) listener, null);
				skipListeners.add(skipListener);
			}
			if(listener instanceof SkipWriteListener) {
				SkipListener<I, O> skipListener = new SkipListenerAdapter<>(null, null, (SkipWriteListener) listener);
				skipListeners.add(skipListener);
			}
			if (listener instanceof StepExecutionListener) {
				StepExecutionListener stepExecutionListener = (StepExecutionListener) listener;
				stepExecutionListeners.add(stepExecutionListener);
			}
			if(listener instanceof javax.batch.api.listener.StepListener) {
				StepExecutionListener stepExecutionListener = new StepListenerAdapter((javax.batch.api.listener.StepListener) listener);
				stepExecutionListeners.add(stepExecutionListener);
			}
			if (listener instanceof ChunkListener) {
				ChunkListener chunkListener = (ChunkListener) listener;
				chunkListeners.add(chunkListener);
			}
			if(listener instanceof javax.batch.api.chunk.listener.ChunkListener) {
				ChunkListener chunkListener = new ChunkListenerAdapter((javax.batch.api.chunk.listener.ChunkListener) listener);
				chunkListeners.add(chunkListener);
			}
			if (listener instanceof ItemReadListener) {
				ItemReadListener<I> readListener = (ItemReadListener<I>) listener;
				readListeners.add(readListener);
			}
			if(listener instanceof javax.batch.api.chunk.listener.ItemReadListener) {
				ItemReadListener<I> itemListener = new ItemReadListenerAdapter<>((javax.batch.api.chunk.listener.ItemReadListener) listener);
				readListeners.add(itemListener);
			}
			if (listener instanceof ItemWriteListener) {
				ItemWriteListener<O> writeListener = (ItemWriteListener<O>) listener;
				writeListeners.add(writeListener);
			}
			if(listener instanceof javax.batch.api.chunk.listener.ItemWriteListener) {
				ItemWriteListener<O> itemListener = new ItemWriteListenerAdapter<>((javax.batch.api.chunk.listener.ItemWriteListener) listener);
				writeListeners.add(itemListener);
			}
			if (listener instanceof ItemProcessListener) {
				ItemProcessListener<I, O> processListener = (ItemProcessListener<I, O>) listener;
				processListeners.add(processListener);
			}
			if(listener instanceof javax.batch.api.chunk.listener.ItemProcessListener) {
				ItemProcessListener<I,O> itemListener = new ItemProcessListenerAdapter<>((javax.batch.api.chunk.listener.ItemProcessListener) listener);
				processListeners.add(itemListener);
			}
			if(listener instanceof RetryReadListener) {
				jsrRetryListeners.add(new RetryReadListenerAdapter((RetryReadListener) listener));
			}
			if(listener instanceof RetryProcessListener) {
				jsrRetryListeners.add(new RetryProcessListenerAdapter((RetryProcessListener) listener));
			}
			if(listener instanceof RetryWriteListener) {
				jsrRetryListeners.add(new RetryWriteListenerAdapter((RetryWriteListener) listener));
			}
			if(listener instanceof PartitionCollector) {
				PartitionCollectorAdapter adapter = new PartitionCollectorAdapter(partitionQueue, (PartitionCollector) listener);
				chunkListeners.add(adapter);
			}
		}
	}

	/**
	 * Exception classes that may not cause a rollback if encountered in the right place.
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
	 * A retry policy to apply when exceptions occur. If this is specified then the retry limit and retryable exceptions
	 * will be ignored.
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
	 * A key generator that can be used to compare items with previously recorded items in a retry. Only used if the
	 * reader is a transactional queue.
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
	 * Public setter for the capacity of the cache in the retry policy. If more items than this fail without being
	 * skipped or recovered an exception will be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems.<br>
	 * <br>
	 * The default value should be high enough and more for most purposes. To breach the limit in a single-threaded step
	 * typically you have to have this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.<br>
	 *
	 * @param cacheCapacity the cache capacity to set (greater than 0 else ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Public setter for the {@link CompletionPolicy} applying to the chunk level. A transaction will be committed when
	 * this policy decides to complete. Defaults to a {@link SimpleCompletionPolicy} with chunk size equal to the
	 * commitInterval property.
	 *
	 * @param chunkCompletionPolicy the chunkCompletionPolicy to set
	 */
	public void setChunkCompletionPolicy(CompletionPolicy chunkCompletionPolicy) {
		this.chunkCompletionPolicy = chunkCompletionPolicy;
	}

	/**
	 * Set the commit interval. Either set this or the chunkCompletionPolicy but not both.
	 *
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	protected Integer getCommitInterval() {
		return this.commitInterval;
	}

	/**
	 * Flag to signal that the reader is transactional (usually a JMS consumer) so that items are re-presented after a
	 * rollback. The default is false and readers are assumed to be forward-only.
	 *
	 * @param isReaderTransactionalQueue the value of the flag
	 */
	public void setIsReaderTransactionalQueue(boolean isReaderTransactionalQueue) {
		this.readerTransactionalQueue = isReaderTransactionalQueue;
	}

	/**
	 * Flag to signal that the processor is transactional, in which case it should be called for every item in every
	 * transaction. If false then we can cache the processor results between transactions in the case of a rollback.
	 *
	 * @param processorTransactional the value to set
	 */
	public void setProcessorTransactional(Boolean processorTransactional) {
		this.processorTransactional = processorTransactional;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this limit. Note this limit includes the
	 * initial attempt to process the item, therefore <code>retryLimit == 1</code> by default.
	 *
	 * @param retryLimit the retry limit to set, must be greater or equal to 1.
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for a limit that determines skip policy. If this value is positive then an exception in chunk
	 * processing will cause the item to be skipped and no exception propagated until the limit is reached. If it is
	 * zero then all exceptions will be propagated from the chunk and cause the step to abort.
	 *
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for a skip policy. If this value is set then the skip limit and skippable exceptions are ignored.
	 *
	 * @param skipPolicy the {@link SkipPolicy} to set
	 */
	public void setSkipPolicy(SkipPolicy skipPolicy) {
		this.skipPolicy = skipPolicy;
	}

	/**
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will be used to execute the chunk processing
	 * inside the {@link Step}.
	 *
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of tasks queued for concurrent processing to prevent
	 * thread pools from being overwhelmed. Defaults to {@link TaskExecutorRepeatTemplate#DEFAULT_THROTTLE_LIMIT}.
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
	 * Public setter for exception classes that when raised won't crash the job but will result in transaction rollback
	 * and the item which handling caused the exception will be skipped.
	 *
	 * @param exceptionClasses {@link Map} containing the {@link Throwable}s as
	 * the keys and the values are {@link Boolean}s, that if true the item is skipped.
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
	 * The streams to inject into the {@link Step}. Any instance of {@link ItemStream} can be used, and will then
	 * receive callbacks at the appropriate stage in the step.
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
	 * @param hasChunkElement true if step has &lt;chunk/&gt; element.
	 */
	public void setHasChunkElement(boolean hasChunkElement) {
		this.hasChunkElement = hasChunkElement;
	}

	/**
	 * @return true if the defined step has a &lt;chunk/&gt; element
	 */
	protected boolean hasChunkElement() {
		return this.hasChunkElement;
	}

	/**
	 * @return true if the defined step has a &lt;tasklet/&gt; element
	 */
	protected boolean hasTasklet() {
		return this.tasklet != null;
	}

	/**
	 * @return true if the defined step has a &lt;partition/&gt; element
	 */
	protected boolean hasPartitionElement() {
		return this.partitionHandler != null;
	}
}
