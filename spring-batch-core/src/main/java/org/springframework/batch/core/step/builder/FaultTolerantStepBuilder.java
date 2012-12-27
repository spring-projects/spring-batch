/*
 * Copyright 2006-2012 the original author or authors.
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
package org.springframework.batch.core.step.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.core.StepListener;
import org.springframework.batch.core.listener.StepListenerFactoryBean;
import org.springframework.batch.core.step.FatalStepExecutionException;
import org.springframework.batch.core.step.item.BatchRetryTemplate;
import org.springframework.batch.core.step.item.ChunkMonitor;
import org.springframework.batch.core.step.item.ChunkOrientedTasklet;
import org.springframework.batch.core.step.item.FaultTolerantChunkProcessor;
import org.springframework.batch.core.step.item.FaultTolerantChunkProvider;
import org.springframework.batch.core.step.item.ForceRollbackForWriteSkipException;
import org.springframework.batch.core.step.item.KeyGenerator;
import org.springframework.batch.core.step.item.SimpleRetryExceptionHandler;
import org.springframework.batch.core.step.skip.CompositeSkipPolicy;
import org.springframework.batch.core.step.skip.ExceptionClassifierSkipPolicy;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NeverSkipItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.skip.SkipPolicyFailedException;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.classify.Classifier;
import org.springframework.classify.SubclassClassifier;
import org.springframework.retry.ExhaustedRetryException;
import org.springframework.retry.RetryException;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.RetryContextCache;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.util.Assert;

/**
 * A step builder for fully fault tolerant chunk-oriented item processing steps. Extends {@link SimpleStepBuilder} with
 * additional properties for retry and skip of failed items.
 * 
 * @author Dave Syer
 * 
 * @since 2.2
 */
public class FaultTolerantStepBuilder<I, O> extends SimpleStepBuilder<I, O> {

	private ChunkMonitor chunkMonitor = new ChunkMonitor();

	private boolean streamIsReader;

	private int retryLimit = 0;

	private BackOffPolicy backOffPolicy;

	private Set<RetryListener> retryListeners = new LinkedHashSet<RetryListener>();

	private RetryPolicy retryPolicy;

	private RetryContextCache retryContextCache;

	private KeyGenerator keyGenerator;

	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses = new LinkedHashSet<Class<? extends Throwable>>();

	private Map<Class<? extends Throwable>, Boolean> skippableExceptionClasses = new HashMap<Class<? extends Throwable>, Boolean>();

	private Collection<Class<? extends Throwable>> nonSkippableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Map<Class<? extends Throwable>, Boolean> retryableExceptionClasses = new HashMap<Class<? extends Throwable>, Boolean>();

	private Collection<Class<? extends Throwable>> nonRetryableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Set<SkipListener<? super I, ? super O>> skipListeners = new LinkedHashSet<SkipListener<? super I, ? super O>>();

	private int skipLimit = 0;

	private SkipPolicy skipPolicy;

	private boolean processorTransactional = true;

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	public FaultTolerantStepBuilder(StepBuilderHelper<?> parent) {
		super(parent);
	}

	/**
	 * Create a new builder initialized with any properties in the parent. The parent is copied, so it can be re-used.
	 * 
	 * @param parent a parent helper containing common step properties
	 */
	protected FaultTolerantStepBuilder(SimpleStepBuilder<I, O> parent) {
		super(parent);
	}

	/**
	 * Create a new chunk oriented tasklet with reader, writer and processor as provided.
	 * 
	 * @see org.springframework.batch.core.step.builder.SimpleStepBuilder#createTasklet()
	 */
	@Override
	protected Tasklet createTasklet() {
		Assert.state(getReader() != null, "ItemReader must be provided");
		Assert.state(getProcessor() != null || getWriter() != null, "ItemWriter or ItemProcessor must be provided");
		addSpecialExceptions();
		registerSkipListeners();
		FaultTolerantChunkProvider<I> chunkProvider = createChunkProvider();
		FaultTolerantChunkProcessor<I, O> chunkProcessor = createChunkProcessor();
		ChunkOrientedTasklet<I> tasklet = new ChunkOrientedTasklet<I>(chunkProvider, chunkProcessor);
		tasklet.setBuffering(!isReaderTransactionalQueue());
		return tasklet;
	}

	/**
	 * Register a skip listener.
	 * 
	 * @param listener the listener to register
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> listener(SkipListener<? super I, ? super O> listener) {
		skipListeners.add(listener);
		return this;
	}

	@Override
	public FaultTolerantStepBuilder<I, O> listener(ChunkListener listener) {
		super.listener(new TerminateOnExceptionChunkListenerDelegate(listener));
		return this;
	}

	@Override
	public AbstractTaskletStepBuilder<SimpleStepBuilder<I, O>> transactionAttribute(
			TransactionAttribute transactionAttribute) {
		return super.transactionAttribute(getTransactionAttribute(transactionAttribute));
	}

	/**
	 * Register a retry listener.
	 * 
	 * @param listener the listener to register
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> listener(RetryListener listener) {
		retryListeners.add(listener);
		return this;
	}

	/**
	 * Sets the key generator for identifying retried items. Retry across transaction boundaries requires items to be
	 * identified when they are encountered again. The default strategy is to use the items themselves, relying on their
	 * own implementation to ensure that they can be identified. Often a key generator is not necessary as long as the
	 * items have reliable hash code and equals implementations, or the reader is not transactional (the default) and
	 * the item processor either is itself not transactional (not the default) or does not create new items.
	 * 
	 * @param keyGenerator a key generator for the stateful retry
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> keyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
		return this;
	}

	/**
	 * The maximum number of times to try a failed item. Zero and one both translate to try only once and do not retry.
	 * Ignored if an explicit {@link #retryPolicy} is set.
	 * 
	 * @param retryLimit the retry limit (default 0)
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> retryLimit(int retryLimit) {
		this.retryLimit = retryLimit;
		return this;
	}

	/**
	 * Provide an explicit retry policy instead of using the {@link #retryLimit(int)} and retryable exceptions provided
	 * elsewhere. Can be used to retry different exceptions a different number of times, for instance.
	 * 
	 * @param retryPolicy a retry policy
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> retryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	/**
	 * Provide a backoff policy to prevent items being retried immediately (e.g. in case the failure was caused by a
	 * remote resource failure that might take some time to be resolved). Ignored if an explicit {@link #retryPolicy} is
	 * set.
	 * 
	 * @param backOffPolicy the back off policy to use (default no backoff)
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> backOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
		return this;
	}

	/**
	 * Provide an explicit retry context cache. Retry is stateful across transactions in the case of failures in item
	 * processing or writing, so some information about the context for subsequent retries has to be stored.
	 * 
	 * @param retryContextCache cache for retry contexts in between transactions (default to standard in-memory
	 * implementation)
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> retryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
		return this;
	}

	/**
	 * Sets the maximium number of failed items to skip before the step fails. Ignored if an explicit
	 * {@link #skipPolicy(SkipPolicy)} is provided.
	 * 
	 * @param skipLimit the skip limit to set
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> skipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
		return this;
	}

	/**
	 * Explicitly prevent certain exceptions (and subclasses) from being skipped.
	 * 
	 * @param type the non-skippable exception
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> noSkip(Class<? extends Throwable> type) {
		skippableExceptionClasses.put(type, false);
		return this;
	}

	/**
	 * Explicitly request certain exceptions (and subclasses) to be skipped.
	 * 
	 * @param type
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> skip(Class<? extends Throwable> type) {
		skippableExceptionClasses.put(type, true);
		return this;
	}

	/**
	 * Provide an explicit policy for managing skips. A skip policy determines which exceptions are skippable and how
	 * many times.
	 * 
	 * @param skipPolicy the skip policy
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> skipPolicy(SkipPolicy skipPolicy) {
		this.skipPolicy = skipPolicy;
		return this;
	}

	/**
	 * Mark this exception as ignorable during item read or processing operations. Processing continues with no
	 * additional callbacks (use skips instead if you need to be notified). Ignored during write because there is no
	 * guarantee of skip and retry without rollback.
	 * 
	 * @param type the exception to mark as no rollback
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> noRollback(Class<? extends Throwable> type) {
		noRollbackExceptionClasses.add(type);
		return this;
	}

	/**
	 * Explicitly ask for an exception (and subclasses) to be excluded from retry.
	 * 
	 * @param type the exception to exclude from retry
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> noRetry(Class<? extends Throwable> type) {
		retryableExceptionClasses.put(type, false);
		return this;
	}

	/**
	 * Explicitly ask for an exception (and subclasses) to be retried.
	 * 
	 * @param type the exception to retry
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> retry(Class<? extends Throwable> type) {
		retryableExceptionClasses.put(type, true);
		return this;
	}

	/**
	 * Mark the item processor as non-transactional (default is the opposite). If this flag is set the results of item
	 * processing are cached across transactions in between retries and during skip processing, otherwise the processor
	 * will be called in every transaction.
	 * 
	 * @return this for fluent chaining
	 */
	public FaultTolerantStepBuilder<I, O> processorNonTransactional() {
		this.processorTransactional = false;
		return this;
	}

	@Override
	public AbstractTaskletStepBuilder<SimpleStepBuilder<I, O>> stream(ItemStream stream) {
		if (stream instanceof ItemReader<?>) {
			if (!streamIsReader) {
				streamIsReader = true;
				super.stream(chunkMonitor);
			}
			// In cases where multiple nested item readers are registered,
			// they all want to get the open() and close() callbacks.
			chunkMonitor.registerItemStream(stream);
		}
		else {
			super.stream(stream);
		}
		return this;
	}

	private FaultTolerantChunkProvider<I> createChunkProvider() {

		SkipPolicy readSkipPolicy = createSkipPolicy();
		readSkipPolicy = getFatalExceptionAwareProxy(readSkipPolicy);
		FaultTolerantChunkProvider<I> chunkProvider = new FaultTolerantChunkProvider<I>(getReader(),
				createChunkOperations());
		chunkProvider.setMaxSkipsOnRead(Math.max(getChunkSize(), FaultTolerantChunkProvider.DEFAULT_MAX_SKIPS_ON_READ));
		chunkProvider.setSkipPolicy(readSkipPolicy);
		chunkProvider.setRollbackClassifier(getRollbackClassifier());
		ArrayList<StepListener> listeners = new ArrayList<StepListener>(getItemListeners());
		listeners.addAll(skipListeners);
		chunkProvider.setListeners(listeners);

		return chunkProvider;

	}

	private FaultTolerantChunkProcessor<I, O> createChunkProcessor() {

		BatchRetryTemplate batchRetryTemplate = createRetryOperations();

		FaultTolerantChunkProcessor<I, O> chunkProcessor = new FaultTolerantChunkProcessor<I, O>(getProcessor(),
				getWriter(), batchRetryTemplate);
		chunkProcessor.setBuffering(!isReaderTransactionalQueue());
		chunkProcessor.setProcessorTransactional(processorTransactional);

		SkipPolicy writeSkipPolicy = createSkipPolicy();
		writeSkipPolicy = getFatalExceptionAwareProxy(writeSkipPolicy);
		chunkProcessor.setWriteSkipPolicy(writeSkipPolicy);
		chunkProcessor.setProcessSkipPolicy(writeSkipPolicy);
		chunkProcessor.setRollbackClassifier(getRollbackClassifier());
		chunkProcessor.setKeyGenerator(keyGenerator);
		detectStreamInReader();

		ArrayList<StepListener> listeners = new ArrayList<StepListener>(getItemListeners());
		listeners.addAll(skipListeners);
		chunkProcessor.setListeners(listeners);
		chunkProcessor.setChunkMonitor(chunkMonitor);

		return chunkProcessor;

	}

	@SuppressWarnings("unchecked")
	private void addSpecialExceptions() {
		addNonSkippableExceptionIfMissing(SkipLimitExceededException.class, NonSkippableReadException.class,
				SkipListenerFailedException.class, SkipPolicyFailedException.class, RetryException.class,
				JobInterruptedException.class, Error.class);
		addNonRetryableExceptionIfMissing(SkipLimitExceededException.class, NonSkippableReadException.class,
				TransactionException.class, FatalStepExecutionException.class, SkipListenerFailedException.class,
				SkipPolicyFailedException.class, RetryException.class, JobInterruptedException.class, Error.class);
	}

	private void detectStreamInReader() {
		if (streamIsReader) {
			if (!concurrent()) {
				chunkMonitor.setItemReader(getReader());
			}
			else {
				logger.warn("Asynchronous TaskExecutor detected with ItemStream reader.  This is probably an error, "
						+ "and may lead to incorrect restart data being stored.");
			}
		}
	}

	/**
	 * Register explicitly set item listeners and auto-register reader, processor and writer if applicable
	 */
	private void registerSkipListeners() {

		// auto-register reader, processor and writer
		for (Object itemHandler : new Object[] { getReader(), getWriter(), getProcessor() }) {

			if (StepListenerFactoryBean.isListener(itemHandler)) {
				StepListener listener = StepListenerFactoryBean.getListener(itemHandler);
				if (listener instanceof SkipListener<?, ?>) {
					@SuppressWarnings("unchecked")
					SkipListener<? super I, ? super O> skipListener = (SkipListener<? super I, ? super O>) listener;
					skipListeners.add(skipListener);
				}
			}

		}
	}

	/**
	 * Convenience method to get an exception classifier based on the provided transaction attributes.
	 * 
	 * @return an exception classifier: maps to true if an exception should cause rollback
	 */
	private Classifier<Throwable, Boolean> getRollbackClassifier() {

		Classifier<Throwable, Boolean> classifier = new BinaryExceptionClassifier(noRollbackExceptionClasses, false);

		// Try to avoid pathological cases where we cannot force a rollback
		// (should be pretty uncommon):
		if (!classifier.classify(new ForceRollbackForWriteSkipException("test", new RuntimeException()))
				|| !classifier.classify(new ExhaustedRetryException("test"))) {

			final Classifier<Throwable, Boolean> binary = classifier;

			Collection<Class<? extends Throwable>> types = new HashSet<Class<? extends Throwable>>();
			types.add(ForceRollbackForWriteSkipException.class);
			types.add(ExhaustedRetryException.class);
			final Classifier<Throwable, Boolean> panic = new BinaryExceptionClassifier(types, true);

			classifier = new Classifier<Throwable, Boolean>() {
                @Override
				public Boolean classify(Throwable classifiable) {
					// Rollback if either the user's list or our own applies
					return panic.classify(classifiable) || binary.classify(classifiable);
				}
			};

		}

		return classifier;

	}

	private TransactionAttribute getTransactionAttribute(TransactionAttribute attribute) {

		final Classifier<Throwable, Boolean> classifier = getRollbackClassifier();
		return new DefaultTransactionAttribute(attribute) {
			@Override
			public boolean rollbackOn(Throwable ex) {
				return classifier.classify(ex);
			}

		};

	}

	protected SkipPolicy createSkipPolicy() {
		SkipPolicy skipPolicy = this.skipPolicy;
		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>(
				skippableExceptionClasses);
		map.put(ForceRollbackForWriteSkipException.class, true);
		LimitCheckingItemSkipPolicy limitCheckingItemSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, map);
		if (skipPolicy == null) {
			Assert.state(!(skippableExceptionClasses.isEmpty() && skipLimit > 0),
					"If a skip limit is provided then skippable exceptions must also be specified");
			skipPolicy = limitCheckingItemSkipPolicy;
		}
		else if (limitCheckingItemSkipPolicy != null) {
			skipPolicy = new CompositeSkipPolicy(new SkipPolicy[] { skipPolicy, limitCheckingItemSkipPolicy });
		}
		return skipPolicy;
	}

	/**
	 * @return fully configured retry template for item processing phase.
	 */
	private BatchRetryTemplate createRetryOperations() {

		RetryPolicy retryPolicy = this.retryPolicy;
		SimpleRetryPolicy simpleRetryPolicy = null;

		Map<Class<? extends Throwable>, Boolean> map = new HashMap<Class<? extends Throwable>, Boolean>(
				retryableExceptionClasses);
		map.put(ForceRollbackForWriteSkipException.class, true);
		simpleRetryPolicy = new SimpleRetryPolicy(retryLimit, map);

		if (retryPolicy == null) {
			Assert.state(!(retryableExceptionClasses.isEmpty() && retryLimit > 0),
					"If a retry limit is provided then retryable exceptions must also be specified");
			retryPolicy = simpleRetryPolicy;
		}
		else if ((!retryableExceptionClasses.isEmpty() && retryLimit > 0)) {
			CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
			compositeRetryPolicy.setPolicies(new RetryPolicy[] { retryPolicy, simpleRetryPolicy });
			retryPolicy = compositeRetryPolicy;
		}

		RetryPolicy retryPolicyWrapper = getFatalExceptionAwareProxy(retryPolicy);

		BatchRetryTemplate batchRetryTemplate = new BatchRetryTemplate();
		if (backOffPolicy != null) {
			batchRetryTemplate.setBackOffPolicy(backOffPolicy);
		}
		batchRetryTemplate.setRetryPolicy(retryPolicyWrapper);

		// Co-ordinate the retry policy with the exception handler:
		RepeatOperations stepOperations = getStepOperations();
		if (stepOperations instanceof RepeatTemplate) {
			SimpleRetryExceptionHandler exceptionHandler = new SimpleRetryExceptionHandler(retryPolicyWrapper,
					getExceptionHandler(), nonRetryableExceptionClasses);
			((RepeatTemplate) stepOperations).setExceptionHandler(exceptionHandler);
		}

		if (retryContextCache != null) {
			batchRetryTemplate.setRetryContextCache(retryContextCache);
		}

		if (retryListeners != null) {
			batchRetryTemplate.setListeners(retryListeners.toArray(new RetryListener[0]));
		}
		return batchRetryTemplate;

	}

	/**
	 * Wrap the provided {@link #setRetryPolicy(RetryPolicy)} so that it never retries explicitly non-retryable
	 * exceptions.
	 */
	private RetryPolicy getFatalExceptionAwareProxy(RetryPolicy retryPolicy) {

		NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();
		Map<Class<? extends Throwable>, RetryPolicy> map = new HashMap<Class<? extends Throwable>, RetryPolicy>();
		for (Class<? extends Throwable> fatal : nonRetryableExceptionClasses) {
			map.put(fatal, neverRetryPolicy);
		}

		SubclassClassifier<Throwable, RetryPolicy> classifier = new SubclassClassifier<Throwable, RetryPolicy>(
				retryPolicy);
		classifier.setTypeMap(map);

		ExceptionClassifierRetryPolicy retryPolicyWrapper = new ExceptionClassifierRetryPolicy();
		retryPolicyWrapper.setExceptionClassifier(classifier);
		return retryPolicyWrapper;

	}

	/**
	 * Wrap a {@link SkipPolicy} and make it consistent with known fatal exceptions.
	 * 
	 * @param skipPolicy an existing skip policy
	 * @return a skip policy that will not skip fatal exceptions
	 */
	private SkipPolicy getFatalExceptionAwareProxy(SkipPolicy skipPolicy) {

		NeverSkipItemSkipPolicy neverSkipPolicy = new NeverSkipItemSkipPolicy();
		Map<Class<? extends Throwable>, SkipPolicy> map = new HashMap<Class<? extends Throwable>, SkipPolicy>();
		for (Class<? extends Throwable> fatal : nonSkippableExceptionClasses) {
			map.put(fatal, neverSkipPolicy);
		}

		SubclassClassifier<Throwable, SkipPolicy> classifier = new SubclassClassifier<Throwable, SkipPolicy>(skipPolicy);
		classifier.setTypeMap(map);

		ExceptionClassifierSkipPolicy skipPolicyWrapper = new ExceptionClassifierSkipPolicy();
		skipPolicyWrapper.setExceptionClassifier(classifier);
		return skipPolicyWrapper;
	}

	private void addNonSkippableExceptionIfMissing(Class<? extends Throwable>... cls) {
		List<Class<? extends Throwable>> exceptions = new ArrayList<Class<? extends Throwable>>();
		for (Class<? extends Throwable> exceptionClass : nonSkippableExceptionClasses) {
			exceptions.add(exceptionClass);
		}
		for (Class<? extends Throwable> fatal : cls) {
			if (!exceptions.contains(fatal)) {
				exceptions.add(fatal);
			}
		}
		nonSkippableExceptionClasses = exceptions;
	}

	private void addNonRetryableExceptionIfMissing(Class<? extends Throwable>... cls) {
		List<Class<? extends Throwable>> exceptions = new ArrayList<Class<? extends Throwable>>();
		for (Class<? extends Throwable> exceptionClass : nonRetryableExceptionClasses) {
			exceptions.add(exceptionClass);
		}
		for (Class<? extends Throwable> fatal : cls) {
			if (!exceptions.contains(fatal)) {
				exceptions.add(fatal);
			}
		}
		nonRetryableExceptionClasses = (List<Class<? extends Throwable>>) exceptions;
	}

	/**
	 * ChunkListener that wraps exceptions thrown from the ChunkListener in {@link FatalStepExecutionException} to force
	 * termination of StepExecution
	 * 
	 * ChunkListeners shoulnd't throw exceptions and expect continued processing, they must be handled in the
	 * implementation or the step will terminate
	 * 
	 */
	private class TerminateOnExceptionChunkListenerDelegate implements ChunkListener {

		private ChunkListener chunkListener;

		TerminateOnExceptionChunkListenerDelegate(ChunkListener chunkListener) {
			this.chunkListener = chunkListener;
		}

        @Override
		public void beforeChunk() {
			try {
				chunkListener.beforeChunk();
			}
			catch (Throwable t) {
				throw new FatalStepExecutionException("ChunkListener threw exception, rethrowing as fatal", t);
			}
		}

        @Override
		public void afterChunk() {
			try {
				chunkListener.afterChunk();
			}
			catch (Throwable t) {
				throw new FatalStepExecutionException("ChunkListener threw exception, rethrowing as fatal", t);
			}
		}

	}

}
