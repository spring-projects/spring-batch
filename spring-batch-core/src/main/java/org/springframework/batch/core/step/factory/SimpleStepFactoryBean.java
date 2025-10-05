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
package org.springframework.batch.core.step.factory;

import io.micrometer.observation.ObservationRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.listener.StepListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.repeat.CompletionPolicy;
import org.springframework.batch.infrastructure.repeat.RepeatOperations;
import org.springframework.batch.infrastructure.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.infrastructure.repeat.exception.ExceptionHandler;
import org.springframework.batch.infrastructure.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.infrastructure.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Most common configuration options for simple steps should be found here. Use this
 * factory bean instead of creating a {@link Step} implementation manually.
 * <p>
 * This factory does not support configuration of fault-tolerant behavior, use appropriate
 * subclass of this factory bean to configure skip or retry.
 *
 * @see FaultTolerantStepFactoryBean
 * @author Dave Syer
 * @author Robert Kasanicky
 * @author Mahmoud Ben Hassine
 * @deprecated Since 6.0 with no replacement. Scheduled for removal in 7.0.
 */
@Deprecated(since = "6.0", forRemoval = true)
public class SimpleStepFactoryBean<T, S> implements FactoryBean<Step>, BeanNameAware {

	private String name;

	private int startLimit = Integer.MAX_VALUE;

	private boolean allowStartIfComplete;

	private ItemReader<? extends T> itemReader;

	private ItemProcessor<? super T, ? extends S> itemProcessor;

	private ItemWriter<? super S> itemWriter;

	private PlatformTransactionManager transactionManager;

	private Propagation propagation = Propagation.REQUIRED;

	private Isolation isolation = Isolation.DEFAULT;

	private int transactionTimeout = DefaultTransactionAttribute.TIMEOUT_DEFAULT;

	protected JobRepository jobRepository;

	protected ObservationRegistry observationRegistry = ObservationRegistry.NOOP;

	private boolean singleton = true;

	private ItemStream[] streams = new ItemStream[0];

	private StepListener[] listeners = new StepListener[0];

	protected final Log logger = LogFactory.getLog(getClass());

	private int commitInterval = 0;

	private TaskExecutor taskExecutor;

	private RepeatOperations stepOperations;

	private RepeatOperations chunkOperations;

	private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

	private CompletionPolicy chunkCompletionPolicy;

	@SuppressWarnings("unused")
	private final int throttleLimit = TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

	private boolean isReaderTransactionalQueue = false;

	/**
	 * Default constructor for {@link SimpleStepFactoryBean}.
	 */
	public SimpleStepFactoryBean() {
		super();
	}

	/**
	 * Flag to signal that the reader is transactional (usually a JMS consumer) so that
	 * items are re-presented after a rollback. The default is false and readers are
	 * assumed to be forward-only.
	 * @param isReaderTransactionalQueue the value of the flag
	 */
	public void setIsReaderTransactionalQueue(boolean isReaderTransactionalQueue) {
		this.isReaderTransactionalQueue = isReaderTransactionalQueue;
	}

	/**
	 * Convenience method for subclasses.
	 * @return true if the flag is set (default false)
	 */
	protected boolean isReaderTransactionalQueue() {
		return isReaderTransactionalQueue;
	}

	/**
	 * Set the bean name property, which will become the name of the {@link Step} when it
	 * is created.
	 *
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Public getter for the name of the step.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * The timeout for an individual transaction in the step.
	 * @param transactionTimeout the transaction timeout to set, defaults to infinite
	 */
	public void setTransactionTimeout(int transactionTimeout) {
		this.transactionTimeout = transactionTimeout;
	}

	/**
	 * @param propagation the propagation to set for business transactions
	 */
	public void setPropagation(Propagation propagation) {
		this.propagation = propagation;
	}

	/**
	 * @param isolation the isolation to set for business transactions
	 */
	public void setIsolation(Isolation isolation) {
		this.isolation = isolation;
	}

	/**
	 * Public setter for the start limit for the step.
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

	/**
	 * Public setter for the flag to indicate that the step should be replayed on a
	 * restart, even if successful the first time.
	 * @param allowStartIfComplete the shouldAllowStartIfComplete to set
	 */
	public void setAllowStartIfComplete(boolean allowStartIfComplete) {
		this.allowStartIfComplete = allowStartIfComplete;
	}

	/**
	 * @param itemReader the {@link ItemReader} to set
	 */
	public void setItemReader(ItemReader<? extends T> itemReader) {
		this.itemReader = itemReader;
	}

	/**
	 * @param itemWriter the {@link ItemWriter} to set
	 */
	public void setItemWriter(ItemWriter<? super S> itemWriter) {
		this.itemWriter = itemWriter;
	}

	/**
	 * @param itemProcessor the {@link ItemProcessor} to set
	 */
	public void setItemProcessor(ItemProcessor<? super T, ? extends S> itemProcessor) {
		this.itemProcessor = itemProcessor;
	}

	/**
	 * The streams to inject into the {@link Step}. Any instance of {@link ItemStream} can
	 * be used, and will then receive callbacks at the appropriate stage in the step.
	 * @param streams an array of listeners
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams = streams;
	}

	/**
	 * The listeners to inject into the {@link Step}. Any instance of {@link StepListener}
	 * can be used, and will then receive callbacks at the appropriate stage in the step.
	 * @param listeners an array of listeners
	 */
	public void setListeners(StepListener[] listeners) {
		this.listeners = listeners;
	}

	/**
	 * Protected getter for the {@link StepListener}s.
	 * @return the listeners
	 */
	protected StepListener[] getListeners() {
		return listeners;
	}

	/**
	 * Protected getter for the {@link ItemReader} for subclasses to use.
	 * @return the itemReader
	 */
	protected ItemReader<? extends T> getItemReader() {
		return itemReader;
	}

	/**
	 * Protected getter for the {@link ItemWriter} for subclasses to use
	 * @return the itemWriter
	 */
	protected ItemWriter<? super S> getItemWriter() {
		return itemWriter;
	}

	/**
	 * Protected getter for the {@link ItemProcessor} for subclasses to use
	 * @return the itemProcessor
	 */
	protected ItemProcessor<? super T, ? extends S> getItemProcessor() {
		return itemProcessor;
	}

	/**
	 * Public setter for {@link JobRepository}.
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for {@link ObservationRegistry}.
	 * @param observationRegistry is an optional dependency (defaults to
	 * {@link ObservationRegistry#NOOP}).
	 */
	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Getter for the {@link TransactionAttribute} for subclasses only.
	 * @return the transactionAttribute
	 */
	protected TransactionAttribute getTransactionAttribute() {

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setPropagationBehavior(propagation.value());
		attribute.setIsolationLevel(isolation.value());
		attribute.setTimeout(transactionTimeout);
		return new DefaultTransactionAttribute(attribute) {

			/**
			 * Ignore the default behaviour and rollback on all exceptions that bubble up
			 * to the tasklet level. The tasklet has to deal with the rollback rules
			 * internally.
			 */
			@Override
			public boolean rollbackOn(Throwable ex) {
				return true;
			}

		};

	}

	/**
	 * Create a {@link Step} from the configuration provided.
	 *
	 * @see FactoryBean#getObject()
	 */
	@Override
	public final Step getObject() throws Exception {
		SimpleStepBuilder<T, S> builder = createBuilder(getName());
		applyConfiguration(builder);
		return builder.build();
	}

	protected SimpleStepBuilder<T, S> createBuilder(String name) {
		return new SimpleStepBuilder<>(new StepBuilder(name, jobRepository));
	}

	@Override
	public Class<TaskletStep> getObjectType() {
		return TaskletStep.class;
	}

	/**
	 * Returns true by default, but in most cases a {@link Step} should <b>not</b> be
	 * treated as thread-safe. Clients are recommended to create a new step for each job
	 * execution.
	 *
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	/**
	 * Public setter for the singleton flag.
	 * @param singleton the value to set. Defaults to true.
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	/**
	 * Set the commit interval. Either set this or the chunkCompletionPolicy but not both.
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}

	/**
	 * Public setter for the {@link CompletionPolicy} applying to the chunk level. A
	 * transaction will be committed when this policy decides to complete. Defaults to a
	 * {@link SimpleCompletionPolicy} with chunk size equal to the commitInterval
	 * property.
	 * @param chunkCompletionPolicy the chunkCompletionPolicy to set
	 */
	public void setChunkCompletionPolicy(CompletionPolicy chunkCompletionPolicy) {
		this.chunkCompletionPolicy = chunkCompletionPolicy;
	}

	/**
	 * Protected getter for the step operations to make them available in subclasses.
	 * @return the step operations
	 */
	protected RepeatOperations getStepOperations() {
		return stepOperations;
	}

	/**
	 * Public setter for the stepOperations.
	 * @param stepOperations the stepOperations to set
	 */
	public void setStepOperations(RepeatOperations stepOperations) {
		this.stepOperations = stepOperations;
	}

	/**
	 * Public setter for the chunkOperations.
	 * @param chunkOperations the chunkOperations to set
	 */
	public void setChunkOperations(RepeatOperations chunkOperations) {
		this.chunkOperations = chunkOperations;
	}

	/**
	 * Protected getter for the chunk operations to make them available in subclasses.
	 * @return the step operations
	 */
	protected RepeatOperations getChunkOperations() {
		return chunkOperations;
	}

	/**
	 * Public setter for the {@link ExceptionHandler}.
	 * @param exceptionHandler the exceptionHandler to set
	 */
	public void setExceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
	}

	/**
	 * Protected getter for the {@link ExceptionHandler}.
	 * @return the {@link ExceptionHandler}
	 */
	protected ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	/**
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will be used to
	 * execute the chunk processing inside the {@link Step}.
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Make the {@link TaskExecutor} available to subclasses
	 * @return the taskExecutor to be used to execute chunks
	 */
	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	protected void applyConfiguration(SimpleStepBuilder<T, S> builder) {

		builder.reader(itemReader);
		builder.processor(itemProcessor);
		builder.writer(itemWriter);
		for (StepExecutionListener listener : BatchListenerFactoryHelper.<StepExecutionListener>getListeners(listeners,
				StepExecutionListener.class)) {
			builder.listener(listener);
		}
		for (ChunkListener listener : BatchListenerFactoryHelper.<ChunkListener>getListeners(listeners,
				ChunkListener.class)) {
			builder.listener(listener);
		}
		for (ItemReadListener<T> listener : BatchListenerFactoryHelper.<ItemReadListener<T>>getListeners(listeners,
				ItemReadListener.class)) {
			builder.listener(listener);
		}
		for (ItemWriteListener<S> listener : BatchListenerFactoryHelper.<ItemWriteListener<S>>getListeners(listeners,
				ItemWriteListener.class)) {
			builder.listener(listener);
		}
		for (ItemProcessListener<T, S> listener : BatchListenerFactoryHelper
			.<ItemProcessListener<T, S>>getListeners(listeners, ItemProcessListener.class)) {
			builder.listener(listener);
		}
		builder.transactionManager(transactionManager);
		builder.transactionAttribute(getTransactionAttribute());
		builder.observationRegistry(observationRegistry);
		builder.startLimit(startLimit);
		builder.allowStartIfComplete(allowStartIfComplete);
		builder.chunk(commitInterval);
		builder.chunk(chunkCompletionPolicy);
		builder.chunkOperations(chunkOperations);
		builder.stepOperations(stepOperations);
		builder.taskExecutor(taskExecutor);
		builder.exceptionHandler(exceptionHandler);
		if (isReaderTransactionalQueue) {
			builder.readerIsTransactionalQueue();
		}
		for (ItemStream stream : streams) {
			builder.stream(stream);
		}

	}

}
