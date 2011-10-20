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
package org.springframework.batch.core.step.item;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.CompletionPolicy;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.policy.SimpleCompletionPolicy;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * Most common configuration options for simple steps should be found here. Use
 * this factory bean instead of creating a {@link Step} implementation manually.
 * 
 * This factory does not support configuration of fault-tolerant behavior, use
 * appropriate subclass of this factory bean to configure skip or retry.
 * 
 * @see FaultTolerantStepFactoryBean
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class SimpleStepFactoryBean<T, S> implements FactoryBean, BeanNameAware {

	private static final int DEFAULT_COMMIT_INTERVAL = 1;

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

	private JobRepository jobRepository;

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

	private int throttleLimit = TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

	private boolean isReaderTransactionalQueue = false;

	/**
	 * Default constructor for {@link SimpleStepFactoryBean}.
	 */
	public SimpleStepFactoryBean() {
		super();
	}

	/**
	 * Flag to signal that the reader is transactional (usually a JMS consumer)
	 * so that items are re-presented after a rollback. The default is false and
	 * readers are assumed to be forward-only.
	 * 
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
	 * Set the bean name property, which will become the name of the
	 * {@link Step} when it is created.
	 * 
	 * @see org.springframework.beans.factory.BeanNameAware#setBeanName(java.lang.String)
	 */
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
	 * 
	 * @param transactionTimeout the transaction timeout to set, defaults to
	 * infinite
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
	 * 
	 * @param startLimit the startLimit to set
	 */
	public void setStartLimit(int startLimit) {
		this.startLimit = startLimit;
	}

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
	 * The streams to inject into the {@link Step}. Any instance of
	 * {@link ItemStream} can be used, and will then receive callbacks at the
	 * appropriate stage in the step.
	 * 
	 * @param streams an array of listeners
	 */
	public void setStreams(ItemStream[] streams) {
		this.streams = streams;
	}

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
	 * 
	 * @param jobRepository is a mandatory dependence (no default).
	 */
	public void setJobRepository(JobRepository jobRepository) {
		this.jobRepository = jobRepository;
	}

	/**
	 * Public setter for the {@link PlatformTransactionManager}.
	 * 
	 * @param transactionManager the transaction manager to set
	 */
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	/**
	 * Getter for the {@link TransactionAttribute} for subclasses only.
	 * @return the transactionAttribute
	 */
	@SuppressWarnings("serial")
	protected TransactionAttribute getTransactionAttribute() {

		DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
		attribute.setPropagationBehavior(propagation.value());
		attribute.setIsolationLevel(isolation.value());
		attribute.setTimeout(transactionTimeout);
		return new DefaultTransactionAttribute(attribute) {

			/**
			 * Ignore the default behaviour and rollback on all exceptions that
			 * bubble up to the tasklet level. The tasklet has to deal with the
			 * rollback rules internally.
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
	public final Object getObject() throws Exception {
		TaskletStep step = new TaskletStep(getName());
		applyConfiguration(step);
		step.afterPropertiesSet();
		return step;
	}

	public Class<TaskletStep> getObjectType() {
		return TaskletStep.class;
	}

	/**
	 * Returns true by default, but in most cases a {@link Step} should not be
	 * treated as thread safe. Clients are recommended to create a new step for
	 * each job execution.
	 * 
	 * @see org.springframework.beans.factory.FactoryBean#isSingleton()
	 */
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
	 * Set the commit interval. Either set this or the chunkCompletionPolicy but
	 * not both.
	 * 
	 * @param commitInterval 1 by default
	 */
	public void setCommitInterval(int commitInterval) {
		this.commitInterval = commitInterval;
	}
	
	/**
	 * Accessor for commit interval if needed in sub classes.
	 * 
	 * @return the commit interval
	 */
	protected int getCommitInterval() {
		return commitInterval;
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
	 * Protected getter for the step operations to make them available in
	 * subclasses.
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
	 * Protected getter for the chunk operations to make them available in
	 * subclasses.
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
	 * Public setter for the {@link TaskExecutor}. If this is set, then it will
	 * be used to execute the chunk processing inside the {@link Step}.
	 * 
	 * @param taskExecutor the taskExecutor to set
	 */
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	/**
	 * Mkae the {@link TaskExecutor} available to subclasses
	 * @return the taskExecutor to be used to execute chunks
	 */
	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	/**
	 * Public setter for the throttle limit. This limits the number of tasks
	 * queued for concurrent processing to prevent thread pools from being
	 * overwhelmed. Defaults to
	 * {@link TaskExecutorRepeatTemplate#DEFAULT_THROTTLE_LIMIT}.
	 * @param throttleLimit the throttle limit to set.
	 */
	public void setThrottleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
	}

	/**
	 * @param step
	 * 
	 */
	protected void applyConfiguration(TaskletStep step) {

		Assert.state(getItemReader()!=null, "ItemReader must be provided");
		Assert.state(getItemWriter()!=null || getItemProcessor()!=null, "ItemWriter or ItemProcessor must be provided");
		Assert.state(transactionManager!=null, "TransactionManager must be provided");

		step.setTransactionManager(transactionManager);
		step.setTransactionAttribute(getTransactionAttribute());
		step.setJobRepository(jobRepository);
		step.setStartLimit(startLimit);
		step.setAllowStartIfComplete(allowStartIfComplete);

		registerStreams(step, streams);

		if (chunkOperations == null) {
			RepeatTemplate repeatTemplate = new RepeatTemplate();
			repeatTemplate.setCompletionPolicy(getChunkCompletionPolicy());
			chunkOperations = repeatTemplate;
		}

		if (stepOperations == null) {

			stepOperations = new RepeatTemplate();

			if (taskExecutor != null) {
				TaskExecutorRepeatTemplate repeatTemplate = new TaskExecutorRepeatTemplate();
				repeatTemplate.setTaskExecutor(taskExecutor);
				repeatTemplate.setThrottleLimit(throttleLimit);
				stepOperations = repeatTemplate;
			}

			((RepeatTemplate) stepOperations).setExceptionHandler(exceptionHandler);

		}

		step.setStepOperations(stepOperations);

		SimpleChunkProvider<T> chunkProvider = configureChunkProvider();

		SimpleChunkProcessor<T, S> chunkProcessor = configureChunkProcessor();

		registerItemListeners(chunkProvider, chunkProcessor);
		registerStepListeners(step, chunkOperations);
		registerStreams(step, itemReader, itemProcessor, itemWriter);

		ChunkOrientedTasklet<T> tasklet = new ChunkOrientedTasklet<T>(chunkProvider, chunkProcessor);
		tasklet.setBuffering(!isReaderTransactionalQueue());

		step.setTasklet(tasklet);

	}

	/**
	 * Register the streams with the step.
	 * @param step the {@link TaskletStep}
	 * @param streams the streams to register
	 */
	protected void registerStreams(TaskletStep step, ItemStream[] streams) {
		step.setStreams(streams);
	}

	/**
	 * Extension point for creating appropriate {@link ChunkProvider}. Return
	 * value must subclass {@link SimpleChunkProvider} due to listener
	 * registration.
	 */
	protected SimpleChunkProvider<T> configureChunkProvider() {
		return new SimpleChunkProvider<T>(itemReader, chunkOperations);
	}

	/**
	 * Extension point for creating appropriate {@link ChunkProcessor}. Return
	 * value must subclass {@link SimpleChunkProcessor} due to listener
	 * registration.
	 */
	protected SimpleChunkProcessor<T, S> configureChunkProcessor() {
		return new SimpleChunkProcessor<T, S>(itemProcessor, itemWriter);
	}

	/**
	 * @return a {@link CompletionPolicy} consistent with the commit interval
	 * and injected policy (if present).
	 */
	private CompletionPolicy getChunkCompletionPolicy() {
		Assert.state(!(chunkCompletionPolicy != null && commitInterval != 0),
				"You must specify either a chunkCompletionPolicy or a commitInterval but not both.");
		Assert.state(commitInterval >= 0, "The commitInterval must be positive or zero (for default value).");

		if (chunkCompletionPolicy != null) {
			return chunkCompletionPolicy;
		}
		if (commitInterval == 0) {
			logger.info("Setting commit interval to default value (" + DEFAULT_COMMIT_INTERVAL + ")");
			commitInterval = DEFAULT_COMMIT_INTERVAL;
		}
		return new SimpleCompletionPolicy(commitInterval);
	}

	private void registerStreams(TaskletStep step, ItemReader<? extends T> itemReader,
			ItemProcessor<? super T, ? extends S> itemProcessor, ItemWriter<? super S> itemWriter) {
		for (Object itemHandler : new Object[] { itemReader, itemWriter, itemProcessor }) {
			if (itemHandler instanceof ItemStream) {
				registerStreams(step, new ItemStream[] { (ItemStream) itemHandler });
			}
		}
	}

	/**
	 * Register listeners with step and chunk.
	 */
	private void registerStepListeners(TaskletStep step, RepeatOperations chunkOperations) {

		for (Object itemHandler : new Object[] { getItemReader(), itemWriter, itemProcessor }) {
			if (StepListenerFactoryBean.isListener(itemHandler)) {
				StepListener listener = StepListenerFactoryBean.getListener(itemHandler);
				if (listener instanceof StepExecutionListener) {
					step.registerStepExecutionListener((StepExecutionListener) listener);
				}
				if (listener instanceof ChunkListener) {
					registerChunkListeners(step, listener);
				}
			}
		}

		step.setStepExecutionListeners(BatchListenerFactoryHelper.getListeners(listeners, StepExecutionListener.class)
				.toArray(new StepExecutionListener[] {}));
		
		List<ChunkListener> chunkListeners = BatchListenerFactoryHelper.getListeners(listeners, ChunkListener.class);
		for(ChunkListener chunkListener: chunkListeners){
			registerChunkListeners(step,chunkListener);
		}
	}

	protected void registerChunkListeners(TaskletStep step, StepListener listener) {
		step.registerChunkListener((ChunkListener) listener);
	}

	/**
	 * Register explicitly set ({@link #setListeners(StepListener[])}) item
	 * listeners and auto-register reader, processor and writer if applicable
	 */
	private void registerItemListeners(SimpleChunkProvider<T> chunkProvider, SimpleChunkProcessor<T, S> chunkProcessor) {

		StepListener[] listeners = getListeners();

		// explicitly set item listeners
		chunkProvider.setListeners(BatchListenerFactoryHelper.<ItemReadListener<T>> getListeners(listeners,
				ItemReadListener.class));
		chunkProvider.setListeners(BatchListenerFactoryHelper.<SkipListener<T, S>> getListeners(listeners,
				SkipListener.class));

		chunkProcessor.setListeners(BatchListenerFactoryHelper.<ItemProcessListener<T, S>> getListeners(listeners,
				ItemProcessListener.class));
		chunkProcessor.setListeners(BatchListenerFactoryHelper.<ItemWriteListener<S>> getListeners(listeners,
				ItemWriteListener.class));
		chunkProcessor.setListeners(BatchListenerFactoryHelper.<SkipListener<T, S>> getListeners(listeners,
				SkipListener.class));

		List<StepListener> listofListeners = Arrays.asList(listeners);
		// auto-register reader, processor and writer
		for (Object itemHandler : new Object[] { getItemReader(), getItemWriter(), getItemProcessor() }) {

			if (listofListeners.contains(itemHandler)) {
				continue;
			}

			if (StepListenerFactoryBean.isListener(itemHandler)) {
				StepListener listener = StepListenerFactoryBean.getListener(itemHandler);
				if (listener instanceof SkipListener<?,?>) {
					chunkProvider.registerListener(listener);
					chunkProcessor.registerListener(listener);
					// already registered with both so avoid double-registering
					continue;
				}
				if (listener instanceof ItemReadListener<?>) {
					chunkProvider.registerListener(listener);
				}
				if (listener instanceof ItemProcessListener<?,?> || listener instanceof ItemWriteListener<?>) {
					chunkProcessor.registerListener(listener);
				}
			}
		}
	}

}