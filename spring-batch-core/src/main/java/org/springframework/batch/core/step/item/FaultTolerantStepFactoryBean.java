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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.springframework.batch.classify.BinaryExceptionClassifier;
import org.springframework.batch.classify.Classifier;
import org.springframework.batch.classify.SubclassClassifier;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.step.skip.LimitCheckingItemSkipPolicy;
import org.springframework.batch.core.step.skip.NonSkippableReadException;
import org.springframework.batch.core.step.skip.SkipLimitExceededException;
import org.springframework.batch.core.step.skip.SkipListenerFailedException;
import org.springframework.batch.core.step.skip.SkipPolicy;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.repeat.RepeatOperations;
import org.springframework.batch.repeat.support.RepeatTemplate;
import org.springframework.batch.retry.ExhaustedRetryException;
import org.springframework.batch.retry.RetryException;
import org.springframework.batch.retry.RetryListener;
import org.springframework.batch.retry.RetryPolicy;
import org.springframework.batch.retry.backoff.BackOffPolicy;
import org.springframework.batch.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.batch.retry.policy.MapRetryContextCache;
import org.springframework.batch.retry.policy.NeverRetryPolicy;
import org.springframework.batch.retry.policy.RetryContextCache;
import org.springframework.batch.retry.policy.SimpleRetryPolicy;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttribute;

/**
 * Factory bean for step that provides options for configuring skip behaviour.
 * User can set {@link #setSkipLimit(int)} to set how many exceptions of
 * {@link #setSkippableExceptionClasses(Collection)} types are tolerated.
 * {@link #setFatalExceptionClasses(Collection)} will cause immediate
 * termination of job - they are treated as higher priority than
 * {@link #setSkippableExceptionClasses(Collection)}, so the two lists don't
 * need to be exclusive.
 * 
 * Skippable exceptions on write will by default cause transaction rollback - to
 * avoid rollback for specific exception class include it in the transaction
 * attribute as "no rollback for".
 * 
 * @see SimpleStepFactoryBean
 * 
 * @author Dave Syer
 * @author Robert Kasanicky
 * 
 */
public class FaultTolerantStepFactoryBean<T, S> extends SimpleStepFactoryBean<T, S> {

	private Collection<Class<? extends Throwable>> skippableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> noRollbackExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> fatalExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> nonRetryableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private Collection<Class<? extends Throwable>> retryableExceptionClasses = new HashSet<Class<? extends Throwable>>();

	private int cacheCapacity = 0;

	private int retryLimit = 0;

	private int skipLimit = 0;

	private BackOffPolicy backOffPolicy;

	private RetryListener[] retryListeners;

	private RetryPolicy retryPolicy;

	private RetryContextCache retryContextCache;

	private KeyGenerator keyGenerator;

	private ChunkMonitor chunkMonitor = new ChunkMonitor();

	/**
	 * The {@link KeyGenerator} to use to identify failed items across rollback.
	 * Not used in the case of the
	 * {@link #setIsReaderTransactionalQueue(boolean) transactional queue flag}
	 * being false (the default).
	 * 
	 * @param keyGenerator the {@link KeyGenerator} to set
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Setter for the retry policy. If this is specified the other retry
	 * properties are ignored (retryLimit, backOffPolicy,
	 * retryableExceptionClasses).
	 * 
	 * @param retryPolicy a stateless {@link RetryPolicy}
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
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
	 * Public setter for the capacity of the cache in the retry policy. If more
	 * items than this fail without being skipped or recovered an exception will
	 * be thrown. This is to guard against inadvertent infinite loops generated
	 * by item identity problems.<br/>
	 * 
	 * The default value should be high enough and more for most purposes. To
	 * breach the limit in a single-threaded step typically you have to have
	 * this many failures in a single transaction. Defaults to the value in the
	 * {@link MapRetryContextCache}.<br/>
	 * 
	 * This property is ignored if the
	 * {@link #setRetryContextCache(RetryContextCache)} is set directly.
	 * 
	 * @param cacheCapacity the cache capacity to set (greater than 0 else
	 * ignored)
	 */
	public void setCacheCapacity(int cacheCapacity) {
		this.cacheCapacity = cacheCapacity;
	}

	/**
	 * Override the default retry context cache for retry of chunk processing.
	 * If this property is set then {@link #setCacheCapacity(int)} is ignored.
	 * 
	 * @param retryContextCache the {@link RetryContextCache} to set
	 */
	public void setRetryContextCache(RetryContextCache retryContextCache) {
		this.retryContextCache = retryContextCache;
	}

	/**
	 * Public setter for the Class[].
	 * @param retryableExceptionClasses the retryableExceptionClasses to set
	 */
	public void setRetryableExceptionClasses(Collection<Class<? extends Throwable>> retryableExceptionClasses) {
		this.retryableExceptionClasses = retryableExceptionClasses;
	}

	/**
	 * Public setter for the {@link BackOffPolicy}.
	 * @param backOffPolicy the {@link BackOffPolicy} to set
	 */
	public void setBackOffPolicy(BackOffPolicy backOffPolicy) {
		this.backOffPolicy = backOffPolicy;
	}

	/**
	 * Public setter for the {@link RetryListener}s.
	 * @param retryListeners the {@link RetryListener}s to set
	 */
	public void setRetryListeners(RetryListener... retryListeners) {
		this.retryListeners = retryListeners;
	}

	/**
	 * A limit that determines skip policy. If this value is positive then an
	 * exception in chunk processing will cause the item to be skipped and no
	 * exception propagated until the limit is reached. If it is zero then all
	 * exceptions will be propagated from the chunk and cause the step to abort.
	 * 
	 * @param skipLimit the value to set. Default is 0 (never skip).
	 */
	public void setSkipLimit(int skipLimit) {
		this.skipLimit = skipLimit;
	}

	/**
	 * Exception classes that when raised won't crash the job but will result in
	 * the item which handling caused the exception being skipped. Any exception
	 * which is marked for "no rollback" is also skippable, but not vice versa.
	 * Remember to set the {@link #setSkipLimit(int) skip limit} as well.
	 * <p/>
	 * Defaults to all exceptions.
	 * 
	 * @param exceptionClasses defaults to <code>Exception</code>
	 */
	public void setSkippableExceptionClasses(Collection<Class<? extends Throwable>> exceptionClasses) {
		this.skippableExceptionClasses = exceptionClasses;
	}

	/**
	 * Exception classes that are candidates for no rollback. The {@link Step}
	 * can not honour the no rollback hint in all circumstances, but any
	 * exception on this list is counted as skippable, so even if there has to
	 * be a rollback, then the step will not fail as long as the skip limit is
	 * not breached.
	 * <p/>
	 * Defaults is empty.
	 * 
	 * @param noRollbackExceptionClasses the exception classes to set
	 */
	public void setNoRollbackExceptionClasses(Collection<Class<? extends Throwable>> noRollbackExceptionClasses) {
		this.noRollbackExceptionClasses = noRollbackExceptionClasses;
	}

	/**
	 * Exception classes that are not skippable (but may be retryable).
	 * 
	 * @param fatalExceptionClasses {@link Error} by default
	 */
	public void setFatalExceptionClasses(Collection<Class<? extends Throwable>> fatalExceptionClasses) {
		this.fatalExceptionClasses = fatalExceptionClasses;
	}

	/**
	 * Convenience method for subclasses to get an exception classifier based on
	 * the provided transaction attributes.
	 * 
	 * @return an exception classifier: maps to true if an exception should
	 * cause rollback
	 */
	protected Classifier<Throwable, Boolean> getRollbackClassifier() {

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
				public Boolean classify(Throwable classifiable) {
					// Rollback if either the user's list or our own applies
					return panic.classify(classifiable) || binary.classify(classifiable);
				}
			};

		}

		return classifier;

	}

	/**
	 * Getter for the {@link TransactionAttribute} for subclasses only.
	 * @return the transactionAttribute
	 */
	@Override
	protected TransactionAttribute getTransactionAttribute() {

		TransactionAttribute attribute = super.getTransactionAttribute();
		final Classifier<Throwable, Boolean> classifier = getRollbackClassifier();
		return new DefaultTransactionAttribute(attribute) {
			@Override
			public boolean rollbackOn(Throwable ex) {
				return classifier.classify(ex);
			}

		};

	}

	@Override
	protected void applyConfiguration(TaskletStep step) {

		addFatalExceptionIfMissing(SkipLimitExceededException.class, NonSkippableReadException.class,
				SkipListenerFailedException.class, RetryException.class, JobInterruptedException.class);
		addNonRetryableExceptionIfMissing(SkipLimitExceededException.class, NonSkippableReadException.class,
				SkipListenerFailedException.class, RetryException.class, JobInterruptedException.class);

		super.applyConfiguration(step);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void registerStreams(TaskletStep step, ItemStream[] streams) {
		boolean streamIsReader = false;
		ItemReader<? extends T> itemReader = getItemReader();
		for (ItemStream stream : streams) {
			if (stream instanceof ItemReader) {
				streamIsReader = true;
				chunkMonitor.registerItemStream(stream);
			}
			else {
				step.registerStream(stream);
			}
		}
		TaskExecutor taskExecutor = getTaskExecutor();
		// In cases where multiple nested item readers are registered,
		// they all want to get the open() and close() callbacks.
		if (streamIsReader) {
			// double registration is fine
			step.registerStream(chunkMonitor);
			boolean concurrent = taskExecutor != null && !(taskExecutor instanceof SyncTaskExecutor);
			if (!concurrent) {
				chunkMonitor.setItemReader(itemReader);
			}
			else {
				logger.warn("Synchronous TaskExecutor detected (" + taskExecutor.getClass()
						+ ") with ItemStream reader.  This is probably an error, "
						+ "and may lead to incorrect restart data being stored.");
			}
		}
	}

	/**
	 * @return {@link ChunkProvider} configured for fault-tolerance.
	 */
	@Override
	protected SimpleChunkProvider<T> configureChunkProvider() {

		SkipPolicy readSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, getSkippableExceptionClasses(),
				fatalExceptionClasses);
		FaultTolerantChunkProvider<T> chunkProvider = new FaultTolerantChunkProvider<T>(getItemReader(),
				getChunkOperations());
		chunkProvider.setSkipPolicy(readSkipPolicy);
		chunkProvider.setRollbackClassifier(getRollbackClassifier());

		return chunkProvider;

	}

	/**
	 * @return {@link ChunkProcessor} configured for fault-tolerance.
	 */
	@Override
	protected SimpleChunkProcessor<T, S> configureChunkProcessor() {

		BatchRetryTemplate batchRetryTemplate = configureRetry();

		FaultTolerantChunkProcessor<T, S> chunkProcessor = new FaultTolerantChunkProcessor<T, S>(getItemProcessor(),
				getItemWriter(), batchRetryTemplate);
		chunkProcessor.setBuffering(!isReaderTransactionalQueue());

		SkipPolicy writeSkipPolicy = new LimitCheckingItemSkipPolicy(skipLimit, getSkippableExceptionClasses(),
				fatalExceptionClasses);
		chunkProcessor.setWriteSkipPolicy(writeSkipPolicy);
		chunkProcessor.setProcessSkipPolicy(writeSkipPolicy);
		chunkProcessor.setRollbackClassifier(getRollbackClassifier());
		chunkProcessor.setKeyGenerator(keyGenerator);
		chunkProcessor.setChunkMonitor(chunkMonitor);

		return chunkProcessor;

	}

	/**
	 * @return
	 */
	private Collection<Class<? extends Throwable>> getSkippableExceptionClasses() {
		HashSet<Class<? extends Throwable>> set = new HashSet<Class<? extends Throwable>>(skippableExceptionClasses);
		set.add(ForceRollbackForWriteSkipException.class);
		return set;
	}

	/**
	 * @return fully configured retry template for item processing phase.
	 */
	private BatchRetryTemplate configureRetry() {

		if (retryPolicy == null) {
			SimpleRetryPolicy simpleRetryPolicy = new SimpleRetryPolicy(retryLimit);
			HashSet<Class<? extends Throwable>> set = new HashSet<Class<? extends Throwable>>(retryableExceptionClasses);
			set.add(ForceRollbackForWriteSkipException.class);
			// set.addAll(noRollbackExceptionClasses); // should only be
			// retryable on write
			simpleRetryPolicy.setRetryableExceptionClasses(set);
			retryPolicy = simpleRetryPolicy;
		}

		RetryPolicy retryPolicyWrapper = fatalExceptionAwareProxy(retryPolicy);

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

		if (retryContextCache == null) {
			if (cacheCapacity > 0) {
				batchRetryTemplate.setRetryContextCache(new MapRetryContextCache(cacheCapacity));
			}
		}
		else {
			batchRetryTemplate.setRetryContextCache(retryContextCache);
		}

		if (retryListeners != null) {
			batchRetryTemplate.setListeners(retryListeners);
		}
		return batchRetryTemplate;
	}

	/**
	 * Wrap the provided {@link #setRetryPolicy(RetryPolicy)} so that it never
	 * retries explicitly non-retryable exceptions.
	 */
	private RetryPolicy fatalExceptionAwareProxy(final RetryPolicy retryPolicy) {

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

	@SuppressWarnings("unchecked")
	private void addFatalExceptionIfMissing(Class... cls) {
		List exceptions = new ArrayList<Class<? extends Throwable>>();
		for (Class exceptionClass : fatalExceptionClasses) {
			exceptions.add(exceptionClass);
		}
		for (Class fatal : cls) {
			if (!exceptions.contains(fatal)) {
				exceptions.add(fatal);
			}
		}
		fatalExceptionClasses = exceptions;
	}

	@SuppressWarnings("unchecked")
	private void addNonRetryableExceptionIfMissing(Class... cls) {
		List exceptions = new ArrayList<Class<? extends Throwable>>();
		for (Class exceptionClass : nonRetryableExceptionClasses) {
			exceptions.add(exceptionClass);
		}
		for (Class fatal : cls) {
			if (!exceptions.contains(fatal)) {
				exceptions.add(fatal);
			}
		}
		nonRetryableExceptionClasses = exceptions;
	}

}
