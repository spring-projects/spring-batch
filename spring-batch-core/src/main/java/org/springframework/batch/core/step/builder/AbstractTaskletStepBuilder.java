/*
 * Copyright 2012-2018 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterChunkError;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.exception.DefaultExceptionHandler;
import org.springframework.batch.repeat.exception.ExceptionHandler;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.repeat.support.TaskExecutorRepeatTemplate;
import org.springframework.batch.support.ReflectionUtils;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Base class for step builders that want to build a {@link TaskletStep}. Handles common concerns across all tasklet
 * step variants, which are mostly to do with the type of tasklet they carry.
 *
 * @author Dave Syer
 * @author Michael Minella
 * @author Mahmoud Ben Hassine
 *
 * @since 2.2
 *
 * @param <B> the type of builder represented
 */
public abstract class AbstractTaskletStepBuilder<B extends AbstractTaskletStepBuilder<B>> extends
StepBuilderHelper<AbstractTaskletStepBuilder<B>> {

	protected Set<ChunkListener> chunkListeners = new LinkedHashSet<>();

	private RepeatOperations stepOperations;

	private TransactionAttribute transactionAttribute;

	private Set<ItemStream> streams = new LinkedHashSet<>();

	private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

	private int throttleLimit = TaskExecutorRepeatTemplate.DEFAULT_THROTTLE_LIMIT;

	private TaskExecutor taskExecutor;

	public AbstractTaskletStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	protected abstract Tasklet createTasklet();

	/**
	 * Build the step from the components collected by the fluent setters. Delegates first to {@link #enhance(Step)} and
	 * then to {@link #createTasklet()} in subclasses to create the actual tasklet.
	 *
	 * @return a tasklet step fully configured and ready to execute
	 */
	public TaskletStep build() {

		registerStepListenerAsChunkListener();

		TaskletStep step = new TaskletStep(getName());

		super.enhance(step);

		step.setChunkListeners(chunkListeners.toArray(new ChunkListener[0]));

		if (transactionAttribute != null) {
			step.setTransactionAttribute(transactionAttribute);
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
		step.setTasklet(createTasklet());

		step.setStreams(streams.toArray(new ItemStream[0]));

		try {
			step.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException(e);
		}

		return step;

	}

	protected void registerStepListenerAsChunkListener() {
		for (StepExecutionListener stepExecutionListener: properties.getStepExecutionListeners()){
			if (stepExecutionListener instanceof ChunkListener){
				listener((ChunkListener)stepExecutionListener);
			}
		}
	}

	/**
	 * Register a chunk listener.
	 *
	 * @param listener the listener to register
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> listener(ChunkListener listener) {
		chunkListeners.add(listener);
		return this;
	}

	/**
	 * Registers objects using the annotation based listener configuration.
	 *
	 * @param listener the object that has a method configured with listener annotation
	 * @return this for fluent chaining
	 */
	@Override
	public B listener(Object listener) {
		super.listener(listener);

		Set<Method> chunkListenerMethods = new HashSet<>();
		chunkListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeChunk.class));
		chunkListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterChunk.class));
		chunkListenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterChunkError.class));

		if(chunkListenerMethods.size() > 0) {
			StepListenerFactoryBean factory = new StepListenerFactoryBean();
			factory.setDelegate(listener);
			this.listener((ChunkListener) factory.getObject());
		}

		@SuppressWarnings("unchecked")
		B result = (B) this;
		return result;
	}

	/**
	 * Register a stream for callbacks that manage restart data.
	 *
	 * @param stream the stream to register
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> stream(ItemStream stream) {
		streams.add(stream);
		return this;
	}

	/**
	 * Provide a task executor to use when executing the tasklet. Default is to use a single-threaded (synchronous)
	 * executor.
	 *
	 * @param taskExecutor the task executor to register
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> taskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
		return this;
	}

	/**
	 * In the case of an asynchronous {@link #taskExecutor(TaskExecutor)} the number of concurrent tasklet executions
	 * can be throttled (beyond any throttling provided by a thread pool). The throttle limit should be less than the
	 * data source pool size used in the job repository for this step.
	 *
	 * @param throttleLimit maximum number of concurrent tasklet executions allowed
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> throttleLimit(int throttleLimit) {
		this.throttleLimit = throttleLimit;
		return this;
	}

	/**
	 * Sets the exception handler to use in the case of tasklet failures. Default is to rethrow everything.
	 *
	 * @param exceptionHandler the exception handler
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> exceptionHandler(ExceptionHandler exceptionHandler) {
		this.exceptionHandler = exceptionHandler;
		return this;
	}

	/**
	 * Sets the repeat template used for iterating the tasklet execution. By default it will terminate only when the
	 * tasklet returns FINISHED (or null).
	 *
	 * @param repeatTemplate a repeat template with rules for iterating
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> stepOperations(RepeatOperations repeatTemplate) {
		this.stepOperations = repeatTemplate;
		return this;
	}

	/**
	 * Sets the transaction attributes for the tasklet execution. Defaults to the default values for the transaction
	 * manager, but can be manipulated to provide longer timeouts for instance.
	 *
	 * @param transactionAttribute a transaction attribute set
	 * @return this for fluent chaining
	 */
	public AbstractTaskletStepBuilder<B> transactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
		return this;
	}

	/**
	 * Convenience method for subclasses to access the step operations that were injected by user.
	 *
	 * @return the repeat operations used to iterate the tasklet executions
	 */
	protected RepeatOperations getStepOperations() {
		return stepOperations;
	}

	/**
	 * Convenience method for subclasses to access the exception handler that was injected by user.
	 *
	 * @return the exception handler
	 */
	protected ExceptionHandler getExceptionHandler() {
		return exceptionHandler;
	}

	/**
	 * Convenience method for subclasses to determine if the step is concurrent.
	 *
	 * @return true if the tasklet is going to be run in multiple threads
	 */
	protected boolean concurrent() {
		boolean concurrent = taskExecutor != null && !(taskExecutor instanceof SyncTaskExecutor);
		return concurrent;
	}

	protected TaskExecutor getTaskExecutor() {
		return taskExecutor;
	}

	protected int getThrottleLimit() {
		return throttleLimit;
	}

	protected TransactionAttribute getTransactionAttribute() {
		return transactionAttribute;
	}

	protected Set<ItemStream> getStreams() {
		return this.streams;
	}

}
