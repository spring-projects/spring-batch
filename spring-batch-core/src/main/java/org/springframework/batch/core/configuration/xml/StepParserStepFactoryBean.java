/*
 * Copyright 2006-2025 the original author or authors.
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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.listener.StepListener;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.partition.PartitionHandler;
import org.springframework.batch.core.partition.Partitioner;
import org.springframework.batch.core.partition.StepExecutionAggregator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.AbstractStep;
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
 * This {@link FactoryBean} is used by the batch namespace parser to create {@link Step}
 * objects. It stores all of the properties that are configurable on the &lt;step/&gt;
 * (and its inner &lt;tasklet/&gt;). Based on which properties are configured, the
 * {@link #getObject()} method delegates to the appropriate class for generating the
 * {@link Step}.
 *
 * @author Dan Garrette
 * @author Josh Long
 * @author Michael Minella
 * @author Chris Schaefer
 * @author Mahmoud Ben Hassine
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

	private final Set<Object> stepExecutionListeners = new LinkedHashSet<>();

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
	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses;

	private Integer transactionTimeout;

	private Propagation propagation;

	private Isolation isolation;

	private final Set<ChunkListener> chunkListeners = new LinkedHashSet<>();

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

	private final Set<ItemReadListener<I>> readListeners = new LinkedHashSet<>();

	private final Set<ItemWriteListener<O>> writeListeners = new LinkedHashSet<>();

	private final Set<ItemProcessListener<I, O>> processListeners = new LinkedHashSet<>();

	private final Set<SkipListener<I, O>> skipListeners = new LinkedHashSet<>();

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
	@Override
	public Step getObject() throws Exception {
		if (hasChunkElement) {
			Assert.isNull(tasklet,
					"Step [" + name + "] has both a <chunk/> element and a 'ref' attribute  referencing a Tasklet.");

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

	/**
	 * Currently, all step implementations other than {@link TaskletStep} are instances of
	 * {@link AbstractStep} and do not require a transaction manager.
	 */
	public boolean requiresTransactionManager() {
		// Currently all step implementations other than TaskletStep are
		// AbstractStep and do not require a transaction manager
		return hasChunkElement || tasklet != null;
	}

	/**
	 * Enhances a step with attributes from the provided {@link StepBuilderHelper}.
	 * @param builder {@link StepBuilderHelper} representing the step to be enhanced
	 */
	protected void enhanceCommonStep(StepBuilderHelper<?> builder) {
		if (allowStartIfComplete != null) {
			builder.allowStartIfComplete(allowStartIfComplete);
		}
		if (startLimit != null) {
			builder.startLimit(startLimit);
		}
		for (Object listener : stepExecutionListeners) {
			if (listener instanceof StepExecutionListener stepExecutionListener) {
				builder.listener(stepExecutionListener);
			}
		}
	}

	/**
	 * Create a partition {@link Step}.
	 * @return the {@link Step}.
	 */
	protected Step createPartitionStep() {

		PartitionStepBuilder builder;
		if (partitioner != null) {
			builder = new StepBuilder(name, jobRepository)
				.partitioner(step != null ? step.getName() : name, partitioner)
				.step(step);
		}
		else {
			builder = new StepBuilder(name, jobRepository).partitioner(step);
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

	/**
	 * Creates a fault tolerant {@link Step}.
	 * @return the {@link Step}.
	 */
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

		if (readerTransactionalQueue != null && readerTransactionalQueue) {
			builder.readerIsTransactionalQueue();
		}

		for (SkipListener<I, O> listener : skipListeners) {
			builder.listener(listener);
		}

		registerItemListeners(builder);

		if (skipPolicy != null) {
			builder.skipPolicy(skipPolicy);
		}
		else if (skipLimit != null) {
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

	/**
	 * Creates a new {@link FaultTolerantStepBuilder}.
	 * @param stepName The name of the step used by the created builder.
	 * @return the {@link FaultTolerantStepBuilder}.
	 */
	protected FaultTolerantStepBuilder<I, O> getFaultTolerantStepBuilder(String stepName) {
		return new FaultTolerantStepBuilder<>(new StepBuilder(stepName, jobRepository));
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

	/**
	 * Creates a new {@link TaskletStep}.
	 * @return the {@link TaskletStep}.
	 */
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
		return new SimpleStepBuilder<>(new StepBuilder(stepName, jobRepository));
	}

	/**
	 * Create a new {@link TaskletStep}.
	 * @return a new {@link TaskletStep}
	 */
	protected TaskletStep createTaskletStep() {
		TaskletStepBuilder builder = new TaskletStepBuilder(new StepBuilder(name, jobRepository)).tasklet(tasklet,
				transactionManager);
		enhanceTaskletStepBuilder(builder);
		return builder.build();
	}

	/**
	 * Set the state of the {@link AbstractTaskletStepBuilder} using the values that were
	 * established for the factory bean.
	 * @param builder The {@link AbstractTaskletStepBuilder} to be modified.
	 */
	protected void enhanceTaskletStepBuilder(AbstractTaskletStepBuilder<?> builder) {

		enhanceCommonStep(builder);
		for (ChunkListener listener : chunkListeners) {
			builder.listener(listener);

		}
		builder.taskExecutor(taskExecutor);
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

	/**
	 * Create a new {@link org.springframework.batch.core.job.flow.FlowStep}.
	 * @return the {@link org.springframework.batch.core.job.flow.FlowStep}.
	 */
	protected Step createFlowStep() {
		FlowStepBuilder builder = new StepBuilder(name, jobRepository).flow(flow);
		enhanceCommonStep(builder);
		return builder.build();
	}

	private Step createJobStep() throws Exception {

		JobStepBuilder builder = new StepBuilder(name, jobRepository).job(job);
		enhanceCommonStep(builder);
		builder.parametersExtractor(jobParametersExtractor);
		builder.launcher(jobLauncher);
		return builder.build();

	}

	/**
	 * Validates that all components required to build a fault tolerant step are set.
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
	 * Check that, if a field is present, then a second field is also present. If the
	 * {@code twoWayDependency} flag is set, the opposite must also be true: if the second
	 * value is present, the first value must also be present.
	 * @param dependentName The name of the first field.
	 * @param dependentValue The value of the first field.
	 * @param name The name of the other field (which should be absent if the first is
	 * present).
	 * @param value The value of the other field.
	 * @param twoWayDependency Set to {@code true} if both depend on each other.
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
	 * @param o An object
	 * @return {@code true} if the object has a value
	 */
	private boolean isPresent(Object o) {
		if (o instanceof Integer i) {
			return isPositive(i);
		}
		if (o instanceof Collection<?> collection) {
			return !collection.isEmpty();
		}
		if (o instanceof Map<?, ?> map) {
			return !map.isEmpty();
		}
		return o != null;
	}

	/**
	 * Indicates whether the step has any components that require fault tolerance.
	 * @return {@code true} if the step is configured with any components that require
	 * fault tolerance.
	 */
	protected boolean isFaultTolerant() {
		return backOffPolicy != null || skipPolicy != null || retryPolicy != null || isPositive(skipLimit)
				|| isPositive(retryLimit) || isPositive(cacheCapacity) || isTrue(readerTransactionalQueue);
	}

	private boolean isTrue(Boolean b) {
		return b != null && b;
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
	 * Set the bean name property, which will become the name of the {@link Step} when it
	 * is created.
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
	 * @return The current step's {@link StepExecutionAggregator}
	 */
	protected StepExecutionAggregator getStepExecutionAggergator() {
		return this.stepExecutionAggregator;
	}

	/**
	 * @param partitionHandler The partitionHandler to set
	 */
	public void setPartitionHandler(PartitionHandler partitionHandler) {
		this.partitionHandler = partitionHandler;
	}

	/**
	 * @return The current step's {@link PartitionHandler}
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
	 * Public setter for the flag to indicate that the step should be replayed on a
	 * restart, even if successful the first time.
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;

	}

	/**
	 * @return The jobRepository
	 */
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * @param jobRepository {@link JobRepository} instance to be used by the step.
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * The number of times that the step should be allowed to start.
	 * @param startLimit int containing the number of times a step should be allowed to
	 * start.
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/**
	 * A preconfigured {@link Tasklet} to use.
	 * @param tasklet {@link Tasklet} instance to be used by step.
	 */
	public void setTasklet(Tasklet tasklet) {
		this.tasklet = tasklet;
	}

	protected Tasklet getTasklet() {
		return this.tasklet;
	}

	/**
	 * @return An instance of {@link PlatformTransactionManager} used by the step.
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
	 * The listeners to inject into the {@link Step}. Any instance of {@link StepListener}
	 * can be used and then receives callbacks at the appropriate stage in the step.
	 * @param listeners An array of listeners
	 */
	@SuppressWarnings("unchecked")
	public void setListeners(Object[] listeners) {
		for (Object listener : listeners) {
			if (listener instanceof SkipListener) {
				SkipListener<I, O> skipListener = (SkipListener<I, O>) listener;
				skipListeners.add(skipListener);
			}
			if (listener instanceof StepExecutionListener stepExecutionListener) {
				stepExecutionListeners.add(stepExecutionListener);
			}
			if (listener instanceof ChunkListener chunkListener) {
				chunkListeners.add(chunkListener);
			}
			if (listener instanceof ItemReadListener) {
				ItemReadListener<I> readListener = (ItemReadListener<I>) listener;
				readListeners.add(readListener);
			}
			if (listener instanceof ItemWriteListener) {
				ItemWriteListener<O> writeListener = (ItemWriteListener<O>) listener;
				writeListeners.add(writeListener);
			}
			if (listener instanceof ItemProcessListener) {
				ItemProcessListener<I, O> processListener = (ItemProcessListener<I, O>) listener;
				processListeners.add(processListener);
			}
		}
	}

	/**
	 * Exception classes that may not cause a rollback if encountered in the right place.
	 * @param noRollbackExceptionClasses The noRollbackExceptionClasses to set
	 */
	public void setNoRollbackExceptionClasses(Collection<Class<? extends Throwable>> noRollbackExceptionClasses) {
		this.noRollbackExceptionClasses = noRollbackExceptionClasses;
	}

	/**
	 * @param transactionTimeout The transactionTimeout to set
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * @param isolation The isolation to set
	 */
	public void setIsolation(Isolation isolation) {
		this.isolation = isolation;
	}

	/**
	 * @param propagation The propagation to set
	 */
	public void setPropagation(Propagation propagation) {
		this.propagation = propagation;
	}

	// =========================================================
	// Parent Attributes - can be provided in parent bean but not namespace
	// =========================================================

	/**
	 * A backoff policy to be applied to the retry process.
	 * @param backOffPolicy The {@link BackOffPolicy} to set
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * A retry policy to apply when exceptions occur. If this is specified then the retry
	 * limit and retryable exceptions will be ignored.
	 * @param retryPolicy the {@link RetryPolicy} to set
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
	}

	/**
	 * @param retryContextCache The {@link RetryContextCache} to set
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * A key generator that can be used to compare items with previously recorded items in
	 * a retry. Used only if the reader is a transactional queue.
	 * @param keyGenerator the {@link KeyGenerator} to set
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	// =========================================================
	// Chunk Attributes
	// =========================================================

	/**
	 *
	 * Public setter for the capacity of the cache in the retry policy. If there are more
	 * items than the specified capacity, the step fails without being skipped or
	 * recovered, and an exception is thrown. This guards against inadvertent infinite
	 * loops generated by item identity problems.<br>
	 * <br>
	 * The default value should be high enough for most purposes. To breach the limit in a
	 * single-threaded step, you typically have to have this many failures in a single
	 * transaction. Defaults to the value in the {@link MapRetryContextCache}.<br>
	 * @param cacheCapacity The cache capacity to set (greater than 0 else ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Public setter for the {@link CompletionPolicy} that applies to the chunk level. A
	 * transaction is committed when this policy decides to complete. Defaults to a
	 * {@link SimpleCompletionPolicy} with chunk size equal to the {@code commitInterval}
	 * property.
	 * @param chunkCompletionPolicy The {@code chunkCompletionPolicy} to set.
	 */
	public void setChunkCompletionPolicy(CompletionPolicy chunkCompletionPolicy) {
		this.chunkCompletionPolicy = chunkCompletionPolicy;
	}

	/**
	 * Set the commit interval. Set either this or the {@code chunkCompletionPolicy} but
	 * not both.
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * @return The commit interval.
	 */
	protected Integer getCommitInterval() {
		return this.commitInterval;
	}

	/**
	 * Flag to signal that the reader is transactional (usually a JMS consumer) so that
	 * items are re-presented after a rollback. The default is {@code false}, and readers
	 * are assumed to be forward-only.
	 * @param isReaderTransactionalQueue the value of the flag
	 */
	public void setIsReaderTransactionalQueue(boolean isReaderTransactionalQueue) {
		this.readerTransactionalQueue = isReaderTransactionalQueue;
	}

	/**
	 * Flag to signal that the processor is transactional -- in that case, it should be
	 * called for every item in every transaction. If {@code false}, we can cache the
	 * processor results between transactions in the case of a rollback.
	 * @param processorTransactional the value to set
	 */
	public void setProcessorTransactional(Boolean processorTransactional) {
		this.processorTransactional = processorTransactional;
	}

	/**
	 * Public setter for the retry limit. Each item can be retried up to this limit. Note
	 * that this limit includes the initial attempt to process the item. Therefore, by
	 * default, <code>retryLimit == 1</code>.
	 * @param retryLimit The retry limit to set. Must be greater than or equal to 1.
	 */
	public void setRetryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
	}

	/**
	 * Public setter for a limit that determines skip policy. If this value is positive,
	 * an exception in chunk processing causes the item to be skipped and no exception to
	 * be propagated until the limit is reached. If it is zero, all exceptions are
	 * propagated from the chunk and cause the step to abort.
	 * @param skipLimit The value to set. The default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Public setter for a skip policy. If this value is set, the skip limit and skippable
	 * exceptions are ignored.
	 * @param skipPolicy The {@link SkipPolicy} to set.
	 */
	public void setSkipPolicy(SkipPolicy skipPolicy) {
		this.skipPolicy = skipPolicy;
	}

	/**
	 * Public setter for the {@link TaskExecutor}. If this is set, it is used to execute
	 * the chunk processing inside the {@link Step}.
	 * @param taskExecutor The taskExecutor to set.
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * @param itemReader The {@link ItemReader} to set.
	 */
	public void setItemReader(ItemReader<? extends I> itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemProcessor The {@link ItemProcessor} to set.
	 */
	public void setItemProcessor(ItemProcessor<? super I, ? extends O> itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/**
	 * @param itemWriter The {@link ItemWriter} to set.
	 */
	public void setItemWriter(ItemWriter<? super O> itemWriter) {
		this.itemWriter = itemWriter;
	}

	// =========================================================
	// Chunk Elements
	// =========================================================

	/**
	 * Public setter for the {@link RetryListener} instances.
	 * @param retryListeners The {@link RetryListener} instances to set.
	 */
	public void setRetryListeners(RetryListener... retryListeners) {
		this.retryListeners = retryListeners;
	}

	/**
	 * Public setter for exception classes that, when raised, do not crash the job but
	 * result in transaction rollback. The item for which handling caused the exception is
	 * skipped.
	 * @param exceptionClasses A {@link Map} containing the {@link Throwable} instances as
	 * the keys and the {@link Boolean} instances as the values. If {@code true}, the item
	 * is skipped.
	 */
	public void setSkippableExceptionClasses(Map<Class<? extends Throwable>, Boolean> exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Public setter for exception classes that retries the item when raised.
	 * @param retryableExceptionClasses The retryableExceptionClasses to set.
	 */
	public void setRetryableExceptionClasses(Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * The streams to inject into the {@link Step}. Any instance of {@link ItemStream} can
	 * be used, and it then receives callbacks at the appropriate stage in the step.
	 * @param streams an array of listeners
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams = streams;
	}

	// =========================================================
	// Additional
	// =========================================================

	/**
	 * @param hasChunkElement {@code true} if step has &lt;chunk/&gt; element.
	 */
	public void setHasChunkElement(boolean hasChunkElement) {
		this.hasChunkElement = hasChunkElement;
	}

	/**
	 * @return {@code true} if the defined step has a &lt;chunk/&gt; element.
	 */
	protected boolean hasChunkElement() {
		return this.hasChunkElement;
	}

	/**
	 * @return {@code true} if the defined step has a &lt;tasklet/&gt; element.
	 */
	protected boolean hasTasklet() {
		return this.tasklet != null;
	}

	/**
	 * @return {@code true} if the defined step has a &lt;partition/&gt; element.
	 */
	protected boolean hasPartitionElement() {
		return this.partitionHandler != null;
	}

}
