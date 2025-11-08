/*
 * Copyright 2025-present the original author or authors.
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
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import io.micrometer.observation.ObservationRegistry;
import org.jspecify.annotations.Nullable;

import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterProcess;
import org.springframework.batch.core.annotation.AfterRead;
import org.springframework.batch.core.annotation.AfterWrite;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.annotation.BeforeProcess;
import org.springframework.batch.core.annotation.BeforeRead;
import org.springframework.batch.core.annotation.BeforeWrite;
import org.springframework.batch.core.annotation.OnChunkError;
import org.springframework.batch.core.annotation.OnProcessError;
import org.springframework.batch.core.annotation.OnReadError;
import org.springframework.batch.core.annotation.OnWriteError;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.core.listener.ItemProcessListener;
import org.springframework.batch.core.listener.ItemReadListener;
import org.springframework.batch.core.listener.ItemWriteListener;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.listener.StepListener;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.StepInterruptionPolicy;
import org.springframework.batch.core.step.ThreadStepInterruptionPolicy;
import org.springframework.batch.core.step.item.ChunkOrientedStep;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingExceptionHierarchySkipPolicy;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemStream;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.support.ReflectionUtils;
import org.springframework.batch.infrastructure.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * A builder for {@link ChunkOrientedStep}. This class extends {@link StepBuilderHelper}
 * to provide common properties and methods for building chunk-oriented steps.
 *
 * @author Mahmoud Ben Hassine
 * @since 6.0
 */
public class ChunkOrientedStepBuilder<I, O> extends StepBuilderHelper<ChunkOrientedStepBuilder<I, O>> {

	private final int chunkSize;

	private @Nullable ItemReader<I> reader;

	private @Nullable ItemProcessor<I, O> processor;

	private @Nullable ItemWriter<O> writer;

	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();

	private TransactionAttribute transactionAttribute = new DefaultTransactionAttribute();

	private final Set<ItemStream> streams = new LinkedHashSet<>();

	private final Set<StepListener> stepListeners = new LinkedHashSet<>();

	private StepInterruptionPolicy interruptionPolicy = new ThreadStepInterruptionPolicy();

	private boolean faultTolerant;

	private @Nullable RetryPolicy retryPolicy;

	private final Set<RetryListener> retryListeners = new LinkedHashSet<>();

	private final Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>();

	private long retryLimit = 0;

	private @Nullable SkipPolicy skipPolicy;

	private final Set<SkipListener<I, O>> skipListeners = new LinkedHashSet<>();

	private final Set<Class<? extends Throwable>> skippableExceptions = new HashSet<>();

	private long skipLimit = 10;

	private @Nullable AsyncTaskExecutor asyncTaskExecutor;

	private @Nullable ObservationRegistry observationRegistry;

	ChunkOrientedStepBuilder(StepBuilderHelper<?> parent, int chunkSize) {
		super(parent);
		this.chunkSize = chunkSize;
	}

	/**
	 * Create a new {@link ChunkOrientedStepBuilder} with the given job repository and
	 * transaction manager. The step name will be assigned to the bean name.
	 * @param jobRepository the job repository
	 * @param chunkSize the size of the chunk to be processed
	 */
	public ChunkOrientedStepBuilder(JobRepository jobRepository, int chunkSize) {
		super(jobRepository);
		this.chunkSize = chunkSize;
	}

	/**
	 * Create a new {@link ChunkOrientedStepBuilder} with the given step name, job
	 * repository and transaction manager.
	 * @param name the step name
	 * @param jobRepository the job repository
	 * @param chunkSize the size of the chunk to be processed
	 */
	public ChunkOrientedStepBuilder(String name, JobRepository jobRepository, int chunkSize) {
		super(name, jobRepository);
		this.chunkSize = chunkSize;
	}

	@Override
	protected ChunkOrientedStepBuilder<I, O> self() {
		return this;
	}

	/**
	 * An item reader that provides a stream of items. Will be automatically registered as
	 * a {@link #stream(ItemStream)} or listener if it implements the corresponding
	 * interface.
	 * @param reader an item reader
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> reader(ItemReader<I> reader) {
		this.reader = reader;
		return self();
	}

	/**
	 * An item processor that processes or transforms a stream of items. Will be
	 * automatically registered as a {@link #stream(ItemStream)} or listener if it
	 * implements the corresponding interface.
	 * @param processor an item processor
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> processor(ItemProcessor<I, O> processor) {
		this.processor = processor;
		return self();
	}

	/**
	 * An item writer that writes a chunk of items. Will be automatically registered as a
	 * {@link #stream(ItemStream)} or listener if it implements the corresponding
	 * interface.
	 * @param writer an item writer
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> writer(ItemWriter<O> writer) {
		this.writer = writer;
		return self();
	}

	/**
	 * Sets the transaction manager to use for the chunk-oriented tasklet. Defaults to a
	 * {@link ResourcelessTransactionManager} if none is provided.
	 * @param transactionManager a transaction manager set
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> transactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
		return self();
	}

	/**
	 * Sets the transaction attributes for the tasklet execution. Defaults to the default
	 * values for the transaction manager, but can be manipulated to provide longer
	 * timeouts for instance.
	 * @param transactionAttribute a transaction attribute set
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> transactionAttribute(TransactionAttribute transactionAttribute) {
		this.transactionAttribute = transactionAttribute;
		return self();
	}

	/**
	 * Register a stream for callbacks that manage restart data.
	 * @param stream the stream to register
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> stream(ItemStream stream) {
		streams.add(stream);
		return self();
	}

	/**
	 * Register an item reader listener.
	 * @param listener the listener to register
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> listener(StepListener listener) {
		this.stepListeners.add(listener);
		return self();
	}

	/**
	 * Registers objects using the annotation-based listener configuration.
	 * @param listener the object that has a method configured with listener annotation(s)
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> listener(Object listener) {
		Set<Method> listenerMethods = new HashSet<>();
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeChunk.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterChunk.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), OnChunkError.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeRead.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterRead.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), OnReadError.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeProcess.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterProcess.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), OnProcessError.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), BeforeWrite.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), AfterWrite.class));
		listenerMethods.addAll(ReflectionUtils.findMethod(listener.getClass(), OnWriteError.class));

		if (!listenerMethods.isEmpty()) {
			StepListenerFactoryBean factory = new StepListenerFactoryBean();
			factory.setDelegate(listener);
			this.stepListeners.add((StepListener) factory.getObject());
		}

		return self();
	}

	/**
	 * Set the interruption policy for the step. This policy determines how the step
	 * handles interruptions, such as when a job is stopped or restarted. The policy is
	 * checked at chunk boundaries to decide whether to continue processing or stop.
	 * Defaults to {@link ThreadStepInterruptionPolicy}.
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> interruptionPolicy(StepInterruptionPolicy interruptionPolicy) {
		this.interruptionPolicy = interruptionPolicy;
		return self();
	}

	/**
	 * Set whether the step is fault-tolerant or not. A fault-tolerant step can handle
	 * failures and continue processing without failing the entire step. This is useful
	 * for scenarios where individual items may fail and be skipped, but the overall step
	 * should still complete successfully. Defaults to false.
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> faultTolerant() {
		this.faultTolerant = true;
		return self();
	}

	/**
	 * Set the retry policy for the step. This policy determines how the step handles
	 * retries in case of failures. It can be used to define the number of retry attempts
	 * and the conditions under which retries should occur. Defaults to no retry policy.
	 * @param retryPolicy the retry policy to use
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> retryPolicy(RetryPolicy retryPolicy) {
		Assert.notNull(retryPolicy, "retryPolicy must not be null");
		this.retryPolicy = retryPolicy;
		return self();
	}

	/**
	 * Add a retry listener to the step. Retry listeners are notified of retry events and
	 * can be used to implement custom retry logic or logging.
	 * @param retryListener the retry listener to add
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> retryListener(RetryListener retryListener) {
		this.retryListeners.add(retryListener);
		return self();
	}

	@SafeVarargs
	public final ChunkOrientedStepBuilder<I, O> retry(Class<? extends Throwable>... retryableExceptions) {
		this.retryableExceptions.addAll(Arrays.stream(retryableExceptions).toList());
		return self();
	}

	/**
	 * Set the retry limit for the step. If no explicit retry exceptions are configured
	 * via {@link #retry(Class[])}, the default is to retry all {@link Exception} types
	 * but not {@link Error} types (e.g., OutOfMemoryError, StackOverflowError). This
	 * ensures that fatal JVM errors fail immediately rather than being retried.
	 * @param retryLimit the maximum number of retry attempts
	 * @return this for fluent chaining
	 * @since 6.0
	 */
	public ChunkOrientedStepBuilder<I, O> retryLimit(long retryLimit) {
		Assert.isTrue(retryLimit > 0, "retryLimit must be positive");
		this.retryLimit = retryLimit;
		return self();
	}

	/**
	 * Set the skip policy for the step. This policy determines how the step handles
	 * skipping items in case of failures. It can be used to define the conditions under
	 * which items should be skipped and how many times an item can be skipped before the
	 * step fails. Defaults to {@link NeverSkipItemSkipPolicy}.
	 * @param skipPolicy the skip policy to use
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> skipPolicy(SkipPolicy skipPolicy) {
		Assert.notNull(skipPolicy, "skipPolicy must not be null");
		this.skipPolicy = skipPolicy;
		return self();
	}

	/**
	 * Add a skip listener to the step. Skip listeners are notified when an item is
	 * skipped due to a failure or an error. They can be used to implement custom skip
	 * logic or logging.
	 * @param skipListener the skip listener to add
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> skipListener(SkipListener<I, O> skipListener) {
		this.skipListeners.add(skipListener);
		return self();
	}

	@SafeVarargs
	public final ChunkOrientedStepBuilder<I, O> skip(Class<? extends Throwable>... skippableExceptions) {
		this.skippableExceptions.addAll(Arrays.stream(skippableExceptions).toList());
		return self();
	}

	/**
	 * Set the skip limit for the step. This limit determines the maximum number of items
	 * that can be skipped before the step fails. If the number of skipped items exceeds
	 * this limit, the step will throw a {@link SkipLimitExceededException} and fail.
	 * Defaults to 10.
	 * @param skipLimit the skip limit to set
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> skipLimit(long skipLimit) {
		Assert.isTrue(skipLimit > 0, "skipLimit must be positive");
		this.skipLimit = skipLimit;
		return self();
	}

	/**
	 * Set the asynchronous task executor to be used for processing items concurrently.
	 * This allows for concurrent processing of items, improving performance and
	 * throughput. If not set, the step will process items sequentially.
	 * @param asyncTaskExecutor the asynchronous task executor to use
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> taskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
		this.asyncTaskExecutor = asyncTaskExecutor;
		return self();
	}

	/**
	 * Set the observation registry to be used for collecting metrics during step
	 * execution. This allows for monitoring and analyzing the performance of the step. If
	 * not set, it will default to {@link ObservationRegistry#NOOP}.
	 * @param observationRegistry the observation registry to use
	 * @return this for fluent chaining
	 */
	public ChunkOrientedStepBuilder<I, O> observationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
		return self();
	}

	@SuppressWarnings("unchecked")
	public ChunkOrientedStep<I, O> build() {
		Assert.notNull(this.reader, "Item reader must not be null");
		Assert.notNull(this.writer, "Item writer must not be null");
		if (this.reader instanceof StepExecutionListener listener) {
			this.stepListeners.add(listener);
		}
		if (this.writer instanceof StepExecutionListener listener) {
			this.stepListeners.add(listener);
		}
		ChunkOrientedStep<I, O> chunkOrientedStep = new ChunkOrientedStep<>(this.getName(), this.chunkSize, this.reader,
				this.writer, this.getJobRepository());
		if (this.processor != null) {
			chunkOrientedStep.setItemProcessor(this.processor);
		}
		if (this.processor instanceof StepExecutionListener listener) {
			this.stepListeners.add(listener);
		}
		chunkOrientedStep.setTransactionManager(this.transactionManager);
		chunkOrientedStep.setTransactionAttribute(this.transactionAttribute);
		chunkOrientedStep.setInterruptionPolicy(this.interruptionPolicy);
		if (this.retryPolicy == null) {
			if (!this.retryableExceptions.isEmpty() || this.retryLimit > 0) {
				// Default to Exception.class when retryLimit is set without explicit
				// retry() configuration. This prevents retrying fatal JVM errors like
				// OutOfMemoryError and StackOverflowError.
				Set<Class<? extends Throwable>> exceptions = this.retryableExceptions.isEmpty()
						? Set.of(Exception.class) : this.retryableExceptions;
				this.retryPolicy = RetryPolicy.builder().maxRetries(this.retryLimit).includes(exceptions).build();
			}
			else {
				this.retryPolicy = throwable -> false;
			}
		}
		chunkOrientedStep.setRetryPolicy(this.retryPolicy);
		if (this.skipPolicy == null) {
			if (!this.skippableExceptions.isEmpty() || this.skipLimit > 0) {
				this.skipPolicy = new LimitCheckingExceptionHierarchySkipPolicy(this.skippableExceptions,
						this.skipLimit);
			}
			else {
				this.skipPolicy = new NeverSkipItemSkipPolicy();
			}
		}
		chunkOrientedStep.setSkipPolicy(this.skipPolicy);
		chunkOrientedStep.setFaultTolerant(this.faultTolerant);
		if (this.asyncTaskExecutor != null) {
			chunkOrientedStep.setTaskExecutor(this.asyncTaskExecutor);
		}
		streams.forEach(chunkOrientedStep::registerItemStream);
		stepListeners.forEach(stepListener -> {
			if (stepListener instanceof ItemReadListener listener) {
				chunkOrientedStep.registerItemReadListener(listener);
			}
			if (stepListener instanceof ItemProcessListener listener) {
				chunkOrientedStep.registerItemProcessListener(listener);
			}
			if (stepListener instanceof ItemWriteListener listener) {
				chunkOrientedStep.registerItemWriteListener(listener);
			}
			if (stepListener instanceof ChunkListener listener) {
				chunkOrientedStep.registerChunkListener(listener);
			}
			if (stepListener instanceof StepExecutionListener listener) {
				chunkOrientedStep.registerStepExecutionListener(listener);
			}
		});
		retryListeners.forEach(chunkOrientedStep::registerRetryListener);
		skipListeners.forEach(chunkOrientedStep::registerSkipListener);
		if (this.observationRegistry != null) {
			chunkOrientedStep.setObservationRegistry(this.observationRegistry);
		}
		try {
			chunkOrientedStep.afterPropertiesSet();
		}
		catch (Exception e) {
			throw new StepBuilderException("Unable to build a chunk-oriented step", e);
		}
		return chunkOrientedStep;
	}

}
